## SBT Stups

SBT-stups is an sbt plugin that contains a set of related tasks for deploying
sbt applications into stups environments. This includes logging creating
`scm-source.json` file, logging into [pierone](https://github.com/zalando-stups/pierone)
and creating a new application version into [kio](https://github.com/zalando-stups/kio)

### Installation

Simply put the following into your `build/plugins.sbt` file

```sbt
addSbtPlugin("org.zalando" % "sbt-stups" % "0.1.0")
```

For this plugin to work, you need to have the following installed on your system and
available in your path

* git
* pierone
* kio
* mai

Since SBT-stups is an autoplugin, you don't need to do anything explicit to enable it

### Usage

This sbt-plugin is deliberately designed to have a small minimal set of tasks which you
integrate with your current sbt build. The plugin has the following tasks

* createScmSource: This creates a `scm-source.json` file. Configure the the `scmSourceDirectory`
to the location of where you want the file to be created. Be default it is created in your base
directory however if you are using something like [sbt-docker](https://github.com/marcuslonnberg/sbt-docker)
you should probably use something like
```sbt
scmSourceDirectory := (stagingDirectory in Universal).value
```
* pierOneLogin: This logs into pierone. This should be done before pushing your repository
into docker, i.e. if you are using the sbt-docker plugin.
```sbt
(dockerPush in docker) <<= (dockerPush in docker) dependsOn pierOneLogin
```
* maiLogin: This runs the `mai` command line tool to set up AWS login credentials. You usually
dont need to use this task directly as `createKioVersion` will call this automatically. Use the 
`maiProfile` setting to configure the `mai` profile you are using
* createKioVersion: This will create a new version in kio of your application. The `kioTeamName`,
`kioApplicationName`, `kioApplicationVersion`,`dockerArtifactName` and `dockerVersion` settings
are used to configure the task

### Example of deployment pipeline

If your version of sbt is at least 0.13.8, you can use the following to deploy your application
in a single command (again assuming you are using sbt-docker)

```sbt
lazy val deploy = taskKey[Unit]("Deploys the application into Kio")

// Make our dockerPush login to pierone first
(dockerPush in docker) <<= (dockerPush in docker) dependsOn pierOneLogin

// Create a docker image, then push into pierone and then create a new version in kio
deploy := Def.sequential(
  docker,
  dockerPush in docker,
  kioTeamName
).value
```

Then you can simply run `deploy` in `sbt`, all you need to do afterwards is to update your
application with `senza`

### Contributing

Please make sure that you format the code using `scalafmt`. You can do this by running `scalafmt` in sbt before committing.
See [scalafmt](https://olafurpg.github.io/scalafmt/) for more info.

### License

Copyright © 2016 Zalando SE

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.