# Detecting and resolving dependency vulnerabilities in Gradle projects

This is a simple project demonstrating how to use the `dependency-submission` GitHub action to detect
vulnerable dependencies in a Gradle project, and various techniques to address these vulnerabilities.

You may find it useful to fork this repository, which will allow you to follow this guide, view and resolve Dependabot alerts.
Note that GitHub Actions workflows are not automatically enabled for repository forks. 
To start the process, you'll need to:
1. Fork the repository
2. Navigate to "Settings -> Advanced Security" and enable Dependency graph and Dependabot alerts (see below)
3. Navigate to the "Actions" tab to enable GitHub Actions workflows
4. Push a commit to the 'main' branch in order to trigger the initial `dependency-submission` workflow to run. A change to the README will be sufficient.

# Setting up the repository to detect vulnerable dependencies

In order to receive alerts about any vulnerable dependencies for this repository:

1. **Dependency graph and Dependabot alerts are enabled**

<img width="800" alt="image" src="https://github.com/user-attachments/assets/978e21bb-8082-4861-98a5-b42a41fa29a8" />

2. **A simple `dependency-submission` workflow is configured to run on any push to the `main` branch**

https://github.com/gradle/github-dependency-submission-demo/blob/a353fcb9c6a17d937f5988d95d10e28f1994fe03/.github/workflows/dependency-submission.yml#L1-L30

Note that the workflow is configured to publish a [Build Scan®](https://scans.gradle.com/#gradle) on each run. 
This Build Scan will contain information about your project structure and dependencies, 
and will be useful later as we attempt to identify and resolve any vulnerable dependencies.

See the [full dependency-submission documentation](https://github.com/gradle/actions/blob/main/dependency-submission/README.md) for more details on adding a dependency-submission workflow.

# Viewing all dependencies for this repository

Executing the `dependency-submission` workflow will submit the full graph of resolved dependencies to your repository. 
You can view all of these dependencies, and search for a particular dependency [via the GitHub dependency graph view](https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/exploring-the-dependencies-of-a-repository#viewing-the-dependency-graph).

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/5ae5331a-4f34-457e-8d34-aa0e5e714309">

Note that all dependencies reported by the action will be tagged with <code>Detected by **GitHub Dependency Graph Gradle Plugin**</code>.


# Viewing vulnerabilities reported for this repository

After executing the `dependency-submission` workflow, the repository has 5 current Dependabot alerts for vulnerable dependencies. 
These are not publicly visible in the repo, but here is the list:

<img width="800" alt="image" src="https://github.com/user-attachments/assets/99120685-bfd3-42c8-81cf-f750a041f7b8" />

# Fixing dependency vulnerabilities

Vulnerable dependencies can be included in your project via a number of different mechanisms, each requiring a different fix.
In the following sections we will step through the process of investigating, isolating and addressing different types of vulnerabilities.

- [Updating a direct dependency to non-vulnerable version](#updating-a-direct-dependency-to-non-vulnerable-version)
- [Updating a vulnerable transitive dependency by updating a direct dependency](#updating-a-vulnerable-transitive-dependency-by-updating-a-direct-dependency)
- [Updating a transitive dependency using a dependency constraint](#updating-a-transitive-dependency-using-a-dependency-constraint)
- [Updating a Plugin classpath dependency using a dependency constraint](#updating-a-plugin-classpath-dependency-using-a-dependency-constraint)

## Updating a direct dependency to non-vulnerable version

In this example repository, a dependency on `org.apache.commons:commons-text:1.9` results in the following
Dependabot alert:

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/fab87384-d5c6-4f9a-8206-75b4256a82c3">

In this simple case, the vulnerable dependency is declared directly in the Gradle project,
and a newer, non-vulnerable version is available. 

The fix is as simple as bumping the version in the project.
[Here is a pull-request](https://github.com/gradle/github-dependency-submission-demo/pull/1/files) that will address this vulnerability.

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/f6821ca1-e5fe-4894-bc5c-0944357cf6df">

## Updating a vulnerable transitive dependency by updating a direct dependency

We see 2 vulnerabilities reported for `org.apache.commons:commons-compress:1.24.0`, like this:

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/fff445b7-96b5-4437-b519-3889cdfb8b5e">

But there isn't anywhere in our Gradle project where we depend on `commons-compress`, which means this vulnerability
must involve a _transitive_ dependency. The first step is to identify which direct dependency is responsible.

The easiest way to do this is with a Gradle Build Scan®, which is why our workflow is
configured to automatically publish a Build Scan for every dependency submission.
You'll find the Build Scan link in the [summary for each workflow run](https://github.com/gradle/github-dependency-submission-demo/actions/runs/8557095342#summary-23448497383).

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/e7aafb1c-6821-4f7f-8800-b5f3ff33ef0a">

By inspecting [the Build Scan for the latest submission on 'main'](https://scans.gradle.com/s/7r7ceoclg3kky/dependencies?dependencies=commons-compress&expandAll&focusedDependency=WzAsMSwyLFswLDAsWzBdXV0&focusedDependencyView=versions), we can see that `commons-compress` is required by `io.minio:minio:8.5.8`, which _is_ a direct dependency of our project.

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/dea559d9-0d54-41df-8cf2-81625d62b0a9">


Searching for newer versions reveals that there's an updated version of `minio` available (`8.5.9`),
and when we update our project to this version, [the commons-compress library is updated to `1.26.0`](https://scans.gradle.com/s/bfkkg66rwrbbw/dependencies?dependencies=commons-compress&expandAll&focusedDependency=WzAsMSw3LFswLDAsWzBdXV0&focusedDependencyView=versions).

Here's [the pull-request](https://github.com/gradle/github-dependency-submission-demo/pull/2/files) that will update the version of `minio`, 
resolving the 2 Dependabot alerts triggered by `org.apache.commons:commons-compress:1.24.0`.

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/56e1290f-b92c-4f35-80cc-5797e56b036b">

## Updating a transitive dependency using a dependency constraint

At times when you won't be able to update a direct dependency to resolve a transitive dependency vulnerability.
This can occur if there isn't a newer version of the direct dependency available, or perhaps your project isn't
compatible with the newer version.

In this case, you can directly control the transitive dependency version using a [dependency constraint](https://docs.gradle.org/current/userguide/dependency_constraints.html).
A dependency contraint allows you to specify a transitive dependency version to use, without adding a direct dependency on that version.

[This pull-request](https://github.com/gradle/github-dependency-submission-demo/pull/3/files) adds a dependency constraint that causes the build 
to use a newer version of `commons-compress`, thereby resolving the Dependabot alert. 

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/c05e16d7-32e6-4b08-987b-9cf1215006e5">

If you inspect [the resulting Build Scan](https://scans.gradle.com/s/feschz3ywyb4c/dependencies?dependencies=commons-compress&expandAll&focusedDependency=WzAsMSw1LFswLDAsWzFdXV0&focusedDependencyView=versions), 
you can see that the version of `commons-compress` is updated, but the version of `minio` is not changed.

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/4b153141-b67c-4852-a2a0-f466cc07fa5e">

## Updating a Plugin classpath dependency using a dependency constraint

In addition to things you declare in a `dependencies` block, any Gradle plugins that you apply to your build
will likely have library dependencies. 
Vulnerabilities in these plugin dependencies are detected by the `dependency-submission` action.

The final 2 Dependabot alerts in this project are due to `com.squareup.okio:okio-jvm:3.2.0` and `com.squareup.okio:okio:3.2.0`. 

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/a035a941-c434-43d7-a2a2-942c139a9cb9">

When [searchinig for 'okio' in the **Dependencies** section of the Build Scan](https://scans.gradle.com/s/feschz3ywyb4c/dependencies?dependencies=okio&expandAll), 
we note that there is a dependency on version `3.6.0` (required by `io.minio:minio:8.5.8`), but there is no dependency on vulnerable version `3.2.0`.

The reason you can't see `3.2.0` is because these vulnerable versions are actually brought in by the `com.github.ben-manes.versions` plugin and are listed separately. 

You can see this by [searching in the **Build Dependencies** section of the Build Scan](https://scans.gradle.com/s/feschz3ywyb4c/build-dependencies?dependencies=okio&expandAll&focusedDependency=WzAsMCwyOSxbMCwwLFsyXV1d&focusedDependencyView=versions) instead.

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/2c0ac67f-1032-4b43-9cff-385fde4a4cfa">

Although vulnerable plugin dependencies like this can be trickier to identify, they can be resolved in much the same way as
regular transitive dependencies, either by updating to a new version of the plugin, or by using a dependency constraint.

In this case there is no newer version of the plugin available, so we must add a dependency constraint to force a newer version of `okio` to be used.

[This pull request](https://github.com/gradle/github-dependency-submission-demo/pull/4/files) adds a dependency constraint to the `buildscript` classpath, 
which fixes the security vulnerablitity in `okio` that is introduced by the `com.github.ben-manes.versions` plugin.

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/f1b3420b-668c-4d24-8e73-7f35e998479e">

The [resulting Build Scan demonstrates](https://scans.gradle.com/s/2hwggowm3vyts/build-dependencies?dependencies=okio&expandAll) that the build dependencies no longer include the vulnerable dependency versions.

<img width="800" alt="image" src="https://github.com/gradle/github-dependency-submission-demo/assets/179734/4dbb6680-3256-42d4-bd65-a0f587b29b4a">

