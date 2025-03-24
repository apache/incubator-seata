<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->
# Contributing to Apache Seata(incubating)

It is warmly welcomed if you have interest to hack on Apache Seata(incubating). Firstly, we encourage this kind of willingness very much. And here is a list of contributing guides for you.

[[中文贡献文档](./CONTRIBUTING_CN.md)]

## Topics

* [Reporting security issues](#reporting-security-issues)
* [Reporting general issues](#reporting-general-issues)
* [Code and doc contribution](#code-and-doc-contribution)
* [Test case contribution](#test-case-contribution)
* [Engage to help anything](#engage-to-help-anything)
* [Code Style](#code-style)

## Reporting security issues

Security issues are always treated seriously. As our usual principle, we discourage anyone to spread security issues publicly. If you find a security issue of Apache Seata(incubating), please do not discuss it in public and do not open a public issue. Instead, we encourage you to send us a private email to  [private@seata.apache.org](mailto:private@seata.apache.org) to report this.

## Reporting general issues

To be honest, we view every user of Seata as a very kind contributor. After using Seata, you may have some feedback for the project. If yes, feel free to open an issue via [NEW ISSUE](https://github.com/apache/incubator-seata/issues/new/choose).

Since we collaborate on the project in a distributed way, we appreciate **WELL-WRITTEN**, **DETAILED**, **EXPLICIT** issue reports. To make the communication more efficient, we discourage duplication of issues. If you find an issue already existing, please add your details in comments under the existing issue instead of creating a new one.

To make the issue details as standard as possible, we setup an [ISSUE TEMPLATE](./.github/ISSUE_TEMPLATE) for issue reporters. Please **BE SURE** to follow the instructions in the template when opening a new issue.

There are a lot of cases where you could open an issue:

* bug report
* feature request
* performance issues
* feature proposal
* feature design
* help wanted
* incomplete documentation
* test improvement
* any questions on project
* and so on

Also we must remind you that while creating a new issue, be sure to remove the sensitive data from your post. Sensitive data could be passwords, secret keys, network locations, private business data and so on.

## Code and doc contribution

Every action to make project Seata better is encouraged. On GitHub, every improvement for Seata could be via a PR (short for pull request).

* If you find a typo, try to fix it!
* If you find a bug, try to fix it!
* If you find some redundant codes, try to remove them!
* If you find some test cases missing, try to add them!
* If you could enhance a feature, please **DO NOT** hesitate!
* If you find code implicit, try to add comments to make it clear!
* If you find code ugly, try to refactor that!
* If you can help to improve documents, it could not be better!
* If you find incorrect documents, try to correct them!
* ...

It's quite impossible to list all of these. Remember one principle:

> WE ARE LOOKING FORWARD TO ANY PR FROM YOU.

Since you are ready to improve Seata with a PR, we suggest you could take a look at the PR rules here.

* [Workspace Preparation](#workspace-preparation)
* [Branch Definition](#branch-definition)
* [Commit Rules](#commit-rules)
* [PR Description](#pr-description)

### Workspace Preparation

To put forward a PR, we assume you have a registered GitHub account. Then you could finish the preparation in the following steps:

1. **FORK** Seata to your repository. To make this work, you just need to click the button Fork in right-left of [apache/incubator-seata](https://github.com/apache/incubator-seata) main page. Then you will end up with your repository in `https://github.com/<your-username>/incubator-seata`, in which `your-username` is your GitHub username.

1. **CLONE** your own repository to develop locally. Use `git clone git@github.com:<your-username>/incubator-seata.git` to clone the repository into your local machine. Then you can create new branches to apply your changes.

1. **Set Remote** upstream to be `git@github.com:apache/incubator-seata.git` using the following two commands:

```bash
git remote add upstream git@github.com:apache/incubator-seata.git
git remote set-url --push upstream no-pushing
```

With this remote setting, you can check your git remote configuration like this:

```shell
$ git remote -v
origin     git@github.com:<your-username>/incubator-seata.git (fetch)
origin     git@github.com:<your-username>/incubator-seata.git (push)
upstream   git@github.com:apache/incubator-seata.git (fetch)
upstream   no-pushing (push)
```

Adding this, we can easily synchronize local branches with upstream branches.

### Branch Definition

Right now we assume every contribution via pull request is for [branch development](https://github.com/apache/incubator-seata/tree/2.x) in Seata. Before contributing, being aware of branch definition would help a lot.

As a contributor, keep in mind that every contribution via pull request is for branch developments. While in project Seata, there are several other branches, we generally call them release branches(such as 0.6.0,0.6.1), feature branches, hotfix branches and master branch.

When officially releasing a version, there will be a release branch and named with the version number.

After the release, we will merge the commits of the release branch into the master branch.

When we find that there is a bug in a certain version, we will decide whether to fix it in a later version or in a specific hotfix version. When we decide to fix in the hotfix version, we will checkout the hotfix branch based on the corresponding release branch, perform code repair and verification, merge it into the development branch and the master branch.

For larger features, we will pull out the feature branch for development and verification.


### Commit Rules

In Seata, we take two rules very seriously when committing:

* [Commit Message](#commit-message)
* [Commit Content](#commit-content)

#### Commit Message

Commit message could help reviewers better understand what the purpose of submitted PR is. It could help accelerate the code review procedure as well. We encourage contributors to use **EXPLICIT** commit message rather than ambiguous ones. In general, we advocate the following commit message type:

* docs: xxxx. For example, "docs: add docs about Seata cluster installation".
* feature: xxxx.For example, "feature: support oracle in AT mode".
* bugfix: xxxx. For example, "bugfix: fix panic when input nil parameter".
* refactor: xxxx. For example, "refactor: simplify to make codes more readable".
* test: xxx. For example, "test: add unit test case for func InsertIntoArray".
* other readable and explicit expression ways.

On the other side, we discourage contributors from committing message like the following ways:

* ~~fix bug~~
* ~~update~~
* ~~add doc~~

If you get lost, please check [How to Write a Git Commit Message](http://chris.beams.io/posts/git-commit/) for more details.

#### Commit Content

Commit content represents all content changes included in one commit. You need to include things in one single commit which could help the reviewer without having to look at another commit. In another words, contents in one single commit should pass the CI to avoid code mess. In brief, there are three minor rules for you to keep in mind:

* avoid very large changes in a commit;
* complete and reviewable for each commit.
* check git config(`user.name`, `user.email`) when committing to ensure that it is associated with your GitHub ID.

```bash
git config --get user.name
git config --get user.email
```

* when submitting a PR, please add a brief description of the current changes to the X.X.X.md file under the 'changes/' folder


In addition, in the code change part, we suggest that all contributors should read the [code style of Seata](#code-style).

Apart from having a standard commit message and appropriate commit content, we do take more emphasis on code review.


### PR Description

PR is the only way to make changes to Seata project files. To help reviewers better understand your changes, PR description should be concise. We encourage contributors to follow the [PR template](./.github/PULL_REQUEST_TEMPLATE.md) to create a pull request.

## Test case contribution

Any test case would be welcomed. Currently, Seata function test cases are high priority.

* For unit test, you need to create a test file named `xxxTest.java` in the test directory of the same module. We recommend you to use the junit5 UT framework

* For integration test, you can put the integration test in the test directory or the seata-test module. It is recommended to use the mockito test framework.

## Engage to help with anything

We choose GitHub as the primary place to collaborate on Seata. So the latest updates of Seata are always here. Although contributions via PR is an explicit way to help, we still call for any other ways.

* reply to other's issues if you could;
* help solve other user's problems;
* help review other's PR design;
* help review other's codes in PR;
* discuss about Seata to make things clearer;
* advocate Seata technology beyond GitHub;
* write blogs on Seata and so on.


## Code Style

Seata code style complies with Alibaba Java Coding Guidelines.


### Guidelines
[Alibaba-Java-Coding-Guidelines](https://alibaba.github.io/Alibaba-Java-Coding-Guidelines/)


### Installing IDE Plugin（not necessary）

*It is not necessary to install it, although you can, if you want to find a problem when you are coding.*


#### idea IDE
[p3c-idea-plugin-install](https://github.com/alibaba/p3c/blob/master/idea-plugin/README.md)

#### eclipse IDE
[p3c-eclipse-plugin-install](https://github.com/alibaba/p3c/blob/master/eclipse-plugin/README.md)


To summarize, **ANY HELP IS CONTRIBUTION AND APPRECIATED.**
