package com.cairn.model.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an LLM model is requested but is not currently loaded in the Swarm.
 *
 * <p>WHY: Maps to 503 Service Unavailable because the specific Ollama model needs to be hot-swapped
 * into VRAM before we can serve the request.
 */
public class ModelNotLoadedException extends CairnException {

  public ModelNotLoadedException(String modelName) {
    super(
        "Model '" + modelName + "' is not currently loaded.",
        HttpStatus.SERVICE_UNAVAILABLE,
        "MODEL_NOT_LOADED");
  }
}
