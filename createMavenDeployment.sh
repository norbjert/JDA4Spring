#!/bin/bash
set -euo pipefail

# Read versions from build.gradle
build_version=$(grep -E "^version\s*=\s*'[^']+'" build.gradle | sed -E "s/.*'([^']+)'.*/\1/")
jda_version=$(grep -E "['\"]net\.dv8tion:JDA:" build.gradle | head -n1 | sed -E "s/.*net\.dv8tion:JDA:([^'\" ]+).*/\1/")

if [[ -z "${build_version}" ]]; then
  echo "Failed to determine build_version from build.gradle"
  exit 1
fi
if [[ -z "${jda_version}" ]]; then
  echo "Failed to determine jda_version from build.gradle"
  exit 1
fi


echo "Building jda4spring version ${build_version} (JDA ${jda_version})"

./gradlew clean assemble

cd ./build || exit
OUT_DIR="xyz/norbjert/jda4spring/${build_version}"
mkdir -p "${OUT_DIR}/"

cp ./libs/jda4spring-${build_version}.jar "${OUT_DIR}/"
cp ./libs/jda4spring-${build_version}-javadoc.jar "${OUT_DIR}/"
cp ./libs/jda4spring-${build_version}-sources.jar "${OUT_DIR}/"

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
      <organizationUrl>https://www.norbjert.xyz</organizationUrl>
    </developer>
  </developers>

  <scm>
    <connection>scm:git:git://github.com/norbjert/JDA4Spring.git</connection>
    <developerConnection>scm:git:ssh://github.com:norbjert/JDA4Spring.git</developerConnection>
    <url>https://github.com/norbjert/JDA4Spring/tree/master</url>
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

# Create a final ZIP bundle with the full Maven path structure
ZIP_NAME="jda4spring-${build_version}-maven-bundle.zip"
rm -f "$ZIP_NAME"
zip -r "$ZIP_NAME" "xyz/norbjert/jda4spring/${build_version}"

echo "Created bundle: $(pwd)/${ZIP_NAME}"
