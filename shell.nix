with (import <nixpkgs> {});
mkShell {
	buildInputs = [
		pkg-config
		meson
		ninja
		boost
		jsoncpp
		jdk17
	];
}
