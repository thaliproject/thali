Right now development is set up in a pretty ugly way. I should probably refactor everything using Sub Modules or at the very least put in some build scripts. But right now I'm more focused on the code. But if we actually get other people in here then fixing this mess will become pri 0 since this is not maintainable as is.

The following basically describes the dependency relationships of the three 'active' projects in this sub-directory:

LiveConnectPrototype -1-> DesktopMiniBlogger -2-> AndroidPrototype
         |-------------------3------------------>

1 - DesktopMiniBlogger depends on the file LiveConnectPrototype.jar being physically in the DesktopMiniBlogger sub-directory. This jar file is produced by:
	One Time Setup - In step 2 below you will need to sign the jar with your own key. http://sickpea.com/2009/7/how-to-self-sign-a-java-applet explains how to generate that key.
	Step 1 - Build the LiveConnectPrototype project, this produces the file LiveConnectPrototype.jar in \LiveConnectPrototype\LiveConnectPrototype\dist.
	Step 2 - Sign the jar file by executing something along the lines of "c:\Program Files (x86)\Java\jdk1.7.0_40\bin\jarsigner.exe" dist\LiveConnectPrototype.jar [the id you choose in setup]. You will then be prompted for your signing key.
	Step 3 - Now copy the signed jar to the DesktopMiniBlogger sub-directory.

2 & 3 - The AndroidPrototype has a dependency on both the DesktopMiniBlogger & the LiveConnectPrototype. Oh joy.
	Step 1 - Copy everything BUT the .jar file from \DesktopMiniBlogger into \AndroidProtoType\MyFirstApp\assets\MicroBlogger
	Step 2 - Copy \LiveConnectPrototype\LiveConnectPrototype\src\fi\* and \LiveConnectPrototype\LiveConnectPrototype\src\com\codeplex\peerly\common\*, \LiveConnectPrototype\LiveConnectPrototype\src\com\codeplex\org\json\* and \AndroidPrototype\MyFirstApp\src. The com.codeplex.peerly.browser contains code that won't compile in Android and com.codeplex.peerly.org.json contains a newer verison of the JSON libraries than included in Android. Sigh.


    
      