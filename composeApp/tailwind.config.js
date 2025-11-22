/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/commonMain/kotlin/**/*.kt',
    './src/jsMain/kotlin/**/*.kt',
    './build/js/packages/composeApp/kotlin/**/*.js'
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
