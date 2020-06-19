/** @format */

import { Notes } from "../models";

/** @format */

export default {
  Query: {
    item: () => {
      console.log("TCL: Notes.count()", Notes.count());
      return { id: "lkasdlkasmmasd32klm", note: "this works!" };
    }
  }
};
