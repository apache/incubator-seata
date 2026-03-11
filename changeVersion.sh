echo "./changeVersion.sh oldVersion newVersion"
echo $1
echo $2
find ./ -name pom.xml | grep -v target | xargs perl -pi -e "s|$1|$2|g"
#find ./ -name Version.java | grep -v target | xargs perl -pi -e "s|$1|$2|g"