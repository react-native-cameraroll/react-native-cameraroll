/** @format */

import { ApolloServer } from "apollo-server";

import typeDefs from "./schema";
import resolvers from "./resolvers";

const server = new ApolloServer({
  typeDefs,
  resolvers,
  engine: {
    apiKey: "service:my-crafts:QWJ0Su3euJ3KhX2O_9ExJQ"
  },
  introspection: true
});

server.listen({ port: process.env.PORT || 4000 }).then(({ url }) => {
  console.log(`🚀  Server ready at ${url}`);
});
