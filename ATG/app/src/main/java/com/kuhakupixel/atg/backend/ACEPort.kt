package com.kuhakupixel.atg.backend


object ACEPort {
    /*
    * if we can, we start ACE server on port found by the apk
    *
    * however its not possible for non rooted method where the apk
    * starts its own service via its own port
    * */
    var defaultPort = 56666
    var defaultStatusPublisherPort = 56667
}