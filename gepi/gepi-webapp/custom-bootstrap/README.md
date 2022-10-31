# Bootstrap CSS Customization for GePI

Bootstrap comes with five grid-layout tier for different screen resolutions. We felt that a sixth tier would help. For this reason, we use some customization following https://getbootstrap.com/docs/5.2/layout/grid/#customizing-the-grid. The customization are just two variable redefinitions in `scss/custom.scss`, namely `$grid-breakpoints` and `$container-max-widths`.

The layout of this directory leverages a local node installation of Bootstrap for import to the custom sass file (see the respective import directives). To compile the custom sass file, follow these steps:

* install a current version of Node.js
* install Sass (the JavaScript-only version is enough so you can install for running with Node.js: `npm install -g sass`)
* execute `node install` to fetch the original Bootstrap files
* execute `npx sass scss/custom.scss css/bootstrap-custom.css`

The resulting file is put into `src/main/resources/META-INF/assets/`. The current customized file does already reside there and only needs to be updated upon further customization.

Note that this is a minimal customization and that the original `bootstrap.css` file is still required. The `bootstrap-custom.css` file is imported into GePI in `Layout.java`.