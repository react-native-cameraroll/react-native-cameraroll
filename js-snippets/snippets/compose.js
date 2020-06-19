const { compose, curry, map } = require("ramda");
const json = require("./dummyObj.json");

const prop = curry((property, object) => object[property]);

const extractItem = compose(prop("childList"), prop("parentList"));
const getImage = compose(prop("imageArr"), prop("item"));

const image = compose(getImage, extractItem);

console.log(image(json));
