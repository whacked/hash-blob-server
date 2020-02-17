with import <nixpkgs> {};

stdenv.mkDerivation rec {
    name = "hash-blob-server";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };

    buildInputs = [
        clojure
        leiningen
    ];

    shellHook = ''
    '';
}
