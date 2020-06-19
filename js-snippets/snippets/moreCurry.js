const { curry } = require("ramda");

const replace = curry((reg, sub, s) => s.replace(reg, sub));

const matchRegx = replace(/jd/g);
const replaceWith = matchRegx("--");
const mainString = replaceWith("jdaslknddjjdjdjdjdj");

console.log(mainString);
