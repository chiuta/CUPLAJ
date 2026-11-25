// swift-tools-version:5.9
import PackageDescription

// CuplajCore: the protocol, crypto, hub and client of CUPLAJ for Apple platforms.
// No UI, no audio hardware: it builds and its tests run on macOS as well as iOS.
let package = Package(
    name: "CuplajCore",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [.library(name: "CuplajCore", targets: ["CuplajCore"])],
    targets: [
        .target(name: "CuplajCore"),
        .testTarget(name: "CuplajCoreTests", dependencies: ["CuplajCore"]),
    ]
)
