@org.springframework.modulith.ApplicationModule(
    id = "tracking-review",
    displayName = "Tracking and review",
    allowedDependencies = {"publication::api", "runner-management::api"}
)
package com.vgrunning.trackingreview;
