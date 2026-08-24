@org.springframework.modulith.ApplicationModule(
    id = "planning",
    displayName = "Planning",
    allowedDependencies = {"classification-segmentation::api", "runner-management::api"}
)
package com.vgrunning.planning;
