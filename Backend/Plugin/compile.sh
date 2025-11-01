OUT=Plugin/target/out
SRC=Plugin/src/main/java
JAVAC_EXPORTS=(
  --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
  --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
)
LIB="Plugin/lib/*"

javac -cp "$LIB" -d "$OUT" "${JAVAC_EXPORTS[@]}" $(find "$SRC" -name "*.java")
jar --create --file Plugin/target/PrintWizardPlugin.jar -C "$OUT" .

echo "Plugin compiled to Plugin/target/PrintWizardPlugin.jar"