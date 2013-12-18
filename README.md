Getting Started
===============

After cloning, initialize git submodules:
  - `git submodule init`
  - `git submodule update`

Set environment variable ANDROID_HOME to your local Android SDK installation.
Fetch the Android SDKs needed by this project:  
  - `$ANDROID_HOME/android update sdk --no-ui --all --filter android-10,android-19`

Install Android SDK into your local maven repository:
  - `git clone https://github.com/mosabua/maven-android-sdk-deployer`
  - `cd maven-android-sdk-deployer`
  - `mvn install -P 2.3.3,4.4`

Detailed explanations on Maven Android project setup can be found in
Sonatype's [Android Application Development with Maven](http://books.sonatype.com/mvnref-book/reference/android-dev.html)


Libraries
=========

  - [Google Guava 12.0.1](https://code.google.com/p/guava-libraries/wiki/Release12)  
      (as Maven dependency)
  - [jsoup 1.7.3](https://github.com/jhy/jsoup/tree/d599990c331989230a6e1d0e4fea9e577022cc9c)  
      (as Maven dependency)
  - [Android-RSS-Reader-Library](https://github.com/matshofman/Android-RSS-Reader-Library)  
      (as git submodule)
