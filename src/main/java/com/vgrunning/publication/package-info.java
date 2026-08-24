@org.springframework.modulith.ApplicationModule(
    id = "publication",
    displayName = "Publication",
    allowedDependencies = {"planning::api", "runner-management::api", "notification-delivery::api"}
)
package com.vgrunning.publication;
