import os
import re

# 1. Overwrite Root build.gradle.kts with the March 2026 Standard (9.1.0)
root_kts_content = """// Official Gault Network - Root Manifold
val compileSdk: Int by extra(36)
val minSdk: Int by extra(24)
val targetSdk: Int by extra(36)
val desugarVersion: String by extra("2.1.4")

plugins {
    id("com.android.application") version "9.1.0" apply false
    id("com.android.library") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
"""
with open("build.gradle.kts", "w") as f:
    f.write(root_kts_content)
print("SUCCESS: Root manifold aligned to AGP 9.1.0.")

# 2. Sanitize Sub-projects (Remove local version overrides)
targets = [
    "app/build.gradle.kts",
    "BiglyBT-Android/core/build.gradle",
    "BiglyBT-Android/core/build.gradle.kts"
]

for target in targets:
    if os.path.exists(target):
        with open(target, "r") as f:
            content = f.read()
        
        # Remove 'version "X.X.X"' from id statements so they inherit from root
        content = re.sub(r'id\("com\.android\.(application|library)"\)\s+version\s+"[^"]+"', r'id("com.android.\1")', content)
        content = re.sub(r"id\s+'com\.android\.(application|library)'\s+version\s+'[^']+'", r"id 'com.android.\1'", content)
        
        # Ensure desugarVersion uses the root property
        if "desugarVersion" in content:
            content = content.replace('$desugarVersion', "${rootProject.ext.get('desugarVersion')}")
            content = content.replace('desugarVersion', "rootProject.ext.get('desugarVersion')")

        with open(target, "w") as f:
            f.write(content)
        print(f"SUCCESS: {target} sanitized.")

