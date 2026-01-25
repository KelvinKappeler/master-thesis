OUT_CLASSES=Examples/target/classes
SRC_DIR=Examples/src/main/java
JAVAC_EXPORTS=(
  -J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
  -J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
)
PLUGIN_JAR=Plugin/target/PrintWizardPlugin.jar
LIB="Plugin/lib/*"
CP="$PLUGIN_JAR;$LIB"

echo "COMPILING..."
#find "$SRC_DIR" -name '*.java' -print0 | xargs -0 javac \
#  "${JAVAC_EXPORTS[@]}" -cp "$CP" -Xplugin:PrintWizardPlugin -d "$OUT_CLASSES"
mapfile -d '' sources < <(find "$SRC_DIR" -name '*.java' -print0)
javac "${JAVAC_EXPORTS[@]}" -cp "$CP" -Xplugin:PrintWizardPlugin -d "$OUT_CLASSES" "${sources[@]}"

echo "RUNNING..."
java -cp "$OUT_CLASSES;$CP" ch.epfl.printwizard.examples.cheese.Main