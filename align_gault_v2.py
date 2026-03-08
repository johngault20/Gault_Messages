import os

# 1. Update Root build.gradle.kts with all required extras
root_kts = "build.gradle.kts"
if os.path.exists(root_kts):
    with open(root_kts, "r") as f:
        content = f.read()
    
    # Define the core Gault properties if they aren't fully set
    extras = [
        'val compileSdk: Int by extra(36)',
        'val minSdk: Int by extra(24)',
        'val targetSdk: Int by extra(36)',
        'val desugarVersion: String by extra("2.1.4")' # The missing pulse
    ]
    
    header = ""
    for ex in extras:
        if ex.split(":")[0] not in content:
            header += ex + "\n"
    
    if header:
        content = header + content
    
    # Ensure buildDir is modernized
    content = content.replace("buildDir", "layout.buildDirectory.asFile.get()")
    
    with open(root_kts, "w") as f:
        f.write(content)
    print("SUCCESS: Root manifold fully aligned with desugarVersion.")

# 2. Fix the specific property lookup in BiglyBT Core
core_gradle = "BiglyBT-Android/core/build.gradle"
if os.path.exists(core_gradle):
    with open(core_gradle, "r") as f:
        lines = f.readlines()
    
    new_lines = []
    for line in lines:
        # Resolve the desugarVersion error specifically
        if "desugarVersion" in line and "$" in line:
            # Replaces variable reference with a direct project lookup
            new_line = line.replace("$desugarVersion", "${rootProject.ext.get('desugarVersion')}")
            new_lines.append(new_line)
        elif "compileSdkVersion" in line:
            new_lines.append("    compileSdk rootProject.ext.get(\"compileSdk\")\n")
        elif "targetSdkVersion" in line:
            new_lines.append("    targetSdk rootProject.ext.get(\"targetSdk\")\n")
        elif "minSdkVersion" in line:
            new_lines.append("    minSdk rootProject.ext.get(\"minSdk\")\n")
        else:
            new_lines.append(line)
            
    with open(core_gradle, "w") as f:
        f.writelines(new_lines)
    print("SUCCESS: Core dependencies synced to the Gault Root.")

