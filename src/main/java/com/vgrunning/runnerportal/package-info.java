@org.springframework.modulith.ApplicationModule(
    id = "runner-portal",
    displayName = "Runner portal",
    allowedDependencies = {"runner-management::api", "publication::api", "tracking-review::api"}
)
package com.vgrunning.runnerportal;
