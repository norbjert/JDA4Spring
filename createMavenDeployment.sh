#!/bin/bash

# a little script to help me create the correct package to upload to maven, you can ignore this
build_version=0.0.4
jda_version=5.0.2

gradle assemble

cd ./build || exit
mkdir -p xyz/norbjert/jda4spring/${build_version}/

cp ./libs/jda4spring-${build_version}.jar xyz/norbjert/jda4spring/${build_version}/
cp ./libs/jda4spring-${build_version}-javadoc.jar xyz/norbjert/jda4spring/${build_version}/
cp ./libs/jda4spring-${build_version}-sources.jar xyz/norbjert/jda4spring/${build_version}/

echo '<project xmlns="http://maven.apache.org/POM/4.0.0"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
         <modelVersion>4.0.0</modelVersion>

         <groupId>xyz.norbjert</groupId>
         <artifactId>jda4spring</artifactId>
         <version>'"${build_version}"'</version>
         <packaging>jar</packaging>

         <name>JDA4Spring</name>
         <description>A spring boot integration for the JDA library</description>
         <url>https://github.com/norbjert/JDA4Spring</url>

         <licenses>
             <license>
                 <name>Apache-2.0</name>
                 <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>
             </license>
         </licenses>

         <developers>
             <developer>
             <name>norbjert</name>
             <email>norbert88@protonmail.com</email>
             <organization>norbjert</organization>
             <organizationUrl>http://www.norbjert.xyz</organizationUrl>
             </developer>
         </developers>

         <scm>
             <connection>scm:git:git://github.com/norbjert/JDA4Spring.git</connection>
             <developerConnection>scm:git:ssh://github.com:norbjert/JDA4Spring.git</developerConnection>
             <url>http://github.com/norbjert/JDA4Spring/tree/master</url>
         </scm>

       <dependencies>

         <dependency>
           <groupId>net.dv8tion</groupId>
           <artifactId>JDA</artifactId>
           <version>'"${jda_version}"'</version>
         </dependency>

       </dependencies>

     </project>
' >> ./xyz/norbjert/jda4spring/${build_version}/jda4spring-${build_version}.pom

#gpg -ab /xyz/xyz/norbjert/jda4spring/$build_version/*

for file in ./xyz/norbjert/jda4spring/${build_version}/*; do
  echo $file
    if [ -f "$file" ] && [[ ! "$file" =~ \.asc$ ]]; then
        md5=$(md5sum "$file" | awk '{print $1}')
        sha1=$(sha1sum "$file" | awk '{print $1}')
        echo "$md5" >> "$file.md5"
        echo "$sha1" >> "$file.sha1"
        gpg -ab "$file"
    fi
done
