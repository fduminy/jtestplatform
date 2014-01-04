## JTestPlatform ##
JTestPlatform is a testing framework built on top of existing java testing framework (junit, mauve, jtreg). The client is sending test queries to a pool of servers.
The pool manages load balancing and test distribution according to the client needs in term of platform (32 or 64 bits CPU, number of cores in the CPU …).

Its main purpose is to be able to test the [JNode](http://www.jnode.org) open source project but it’s actually independant of JNode and should be usable for other JVM implementations. It should even be usable for other needs.

## License ##
JTestPlatform's license is GPL 3.0 (see https://github.com/fduminy/jtestplatform/blob/master/LICENSE.txt)
