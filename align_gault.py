import os

# 1. Fix Root build.gradle.kts (Kotlin DSL)
root_kts = "build.gradle.kts"
if os.path.exists(root_kts):
    with open(root_kts, "r") as f:
        content = f.read()
    # Add extra properties if missing
    if "val compileSdk" not in content:
        content = 'val compileSdk: Int by extra(36)\nval minSdk: Int by extra(24)\nval targetSdk: Int by extra(36)\n' + content
    # Replace deprecated buildDir
    content = content.replace("buildDir", "layout.buildDirectory.asFile.get()")
    with open(root_kts, "w") as f:
        f.write(content)
    print("SUCCESS: Root build.gradle.kts aligned.")

# 2. Fix BiglyBT Core build.gradle (Groovy DSL)
core_gradle = "BiglyBT-Android/core/build.gradle"
if os.path.exists(core_gradle):
    with open(core_gradle, "r") as f:
        lines = f.readlines()
    new_lines = []
    for line in lines:
        # Replace compileSdkVersion with compileSdk and point to root extra
        if "compileSdkVersion" in line:
            new_lines.append("    compileSdk rootProject.ext.get(\"compileSdk\")\n")
        elif "targetSdkVersion" in line:
            new_lines.append("    targetSdk rootProject.ext.get(\"targetSdk\")\n")
        elif "minSdkVersion" in line:
            new_lines.append("    minSdk rootProject.ext.get(\"minSdk\")\n")
        else:
            new_lines.append(line)
    with open(core_gradle, "w") as f:
        f.writelines(new_lines)
    print("SUCCESS: BiglyBT Core aligned with Gault SDK standards.")

# 3. Suppress Jetifier Warning in gradle.properties
props = "gradle.properties"
if os.path.exists(props):
    with open(props, "a") as f:
        f.write("\nandroid.enableJetifier=true\n")
    print("SUCCESS: Jetifier enabled for legacy compatibility.")
