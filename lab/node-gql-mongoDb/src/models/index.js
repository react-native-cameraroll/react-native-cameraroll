/** @format */

import { MongoClient } from "mongodb";

import { MONGO_DB_URL } from "../config";

// setup
const client = MongoClient.connect(MONGO_DB_URL);
const db = client.db("links");

// collections/models(?)
const Notes = db.createCollection("notes");

export { Notes };
