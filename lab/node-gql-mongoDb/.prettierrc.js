module.exports = {
    printWidth: 80,
    tabWidth: 2,
    useTabs: false,
    semi: true,
    singleQuote: false,
    trailingComma: "none",
    arrowParens: "always",
    insertPragma: true,
    overrides: [{
        files: "package.json",
        options: {
            parser: "json"
        }
    }]
};