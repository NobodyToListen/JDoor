import { handleRequest } from "./handler.js";

export default {
  fetch(request, env) {
    return handleRequest(request, env);
  }
} satisfies ExportedHandler<Env>;
