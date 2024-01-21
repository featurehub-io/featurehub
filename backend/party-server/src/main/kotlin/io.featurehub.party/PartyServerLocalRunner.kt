package io.featurehub.party

import bathe.BatheBooter

class PartyServerLocalRunner {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      BatheBooter().run(
        arrayOf(
          "-R" + Application::class.java.name,
          "-Pclasspath:/application.properties",
          "-P\${user.home}/.featurehub/fhos-common.properties",
          "-P\${user.home}/.featurehub/party.properties"
        )
      )
    }
  }
}
