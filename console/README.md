# console frontend build note
1. If you upgraded the version number, change `src/resources/version.json`
2. Run console build will automatically build the saga-designer and copy it to the console directory.Make sure that both projects can run in the same node version!
3. The version.json file is generatable. If you need to generate this file automatically, set the value of VERSION in the node environment variable.