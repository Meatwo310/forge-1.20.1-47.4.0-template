{
  description = "Development environment for custom-mdk";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      supportedSystems = [
        "aarch64-linux"
        "x86_64-linux"
      ];
      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
    in
    {
      devShells = forAllSystems (
        system:
        let
          pkgs = import nixpkgs { inherit system; };
          jdk = pkgs.jdk25;
        in
        {
          default = pkgs.mkShell {
            packages = [ jdk ];
            JAVA_HOME = "${jdk}/lib/openjdk";
            JAVA_TOOL_OPTIONS =
              "-Dorg.gradle.java.installations.auto-download=true "
              + "-Dorg.gradle.project.org.gradle.java.installations.auto-download=true";
          };
        }
      );
    };
}
