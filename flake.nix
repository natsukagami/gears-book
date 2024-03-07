{
  inputs = {
    flake-parts.url = "github:hercules-ci/flake-parts";
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = inputs@{ flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      imports = [
        # To import a flake module
        # 1. Add foo to inputs
        # 2. Add foo as a parameter to the outputs function
        # 3. Add here: foo.flakeModule

      ];
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" "x86_64-darwin" ];
      perSystem = { config, self', inputs', pkgs, system, ... }: {
        # Per-system attributes can be defined here. The self' and inputs'
        # module parameters provide easy access to attributes of the same
        # system.

        # Equivalent to  inputs'.nixpkgs.legacyPackages.hello;
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "gears-book";
          version = "0.1.0";
          src = ./.;
          buildInputs = with pkgs; [ mdbook ];
          buildPhase = ''
            mdbook build -d ./book
          '';
          installPhase = ''
            cp -r ./book $out
          '';
        };

        devShells.default = pkgs.mkShell {
          inputsFrom = [ self'.packages.default ];
          buildInputs = with pkgs; (
            let
              jre = jdk21;
            in
            [
              # Scala stuff
              (scala-cli.override { inherit jre; })
              metals
              # Scala Native stuff
              llvm
              clang
              boehmgc
            ]
          );
        };
      };
      flake = {
        # The usual flake attributes can be defined here, including system-
        # agnostic ones like nixosModule and system-enumerating ones, although
        # those are more easily expressed in perSystem.

      };
    };
}
