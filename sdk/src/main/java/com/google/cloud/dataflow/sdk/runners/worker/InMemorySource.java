/*******************************************************************************
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package com.google.cloud.dataflow.sdk.runners.worker;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static com.google.cloud.dataflow.sdk.runners.worker.SourceTranslationUtils.cloudPositionToSourcePosition;
import static com.google.cloud.dataflow.sdk.runners.worker.SourceTranslationUtils.cloudProgressToSourceProgress;
import static com.google.cloud.dataflow.sdk.runners.worker.SourceTranslationUtils.sourceProgressToCloudProgress;
import static java.lang.Math.min;

import com.google.api.services.dataflow.model.ApproximateProgress;
import com.google.cloud.dataflow.sdk.coders.Coder;
import com.google.cloud.dataflow.sdk.util.CoderUtils;
import com.google.cloud.dataflow.sdk.util.StringUtils;
import com.google.cloud.dataflow.sdk.util.common.worker.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

/**
 * A source that yields a set of precomputed elements.
 *
 * @param <T> the type of the elements read from the source
 */
public class InMemorySource<T> extends Source<T> {
  private static final Logger LOG = LoggerFactory.getLogger(InMemorySource.class);

  final List<String> encodedElements;
  final int startIndex;
  final int endIndex;
  final Coder<T> coder;

  public InMemorySource(List<String> encodedElements,
                        @Nullable Long startIndex,
                        @Nullable Long endIndex,
                        Coder<T> coder) {
    this.encodedElements = encodedElements;
    int maxIndex = encodedElements.size();
    if (startIndex == null) {
      this.startIndex = 0;
    } else {
      if (startIndex < 0) {
        throw new IllegalArgumentException("start index should be >= 0");
      }
      this.startIndex = (int) min(startIndex, maxIndex);
    }
    if (endIndex == null) {
      this.endIndex = maxIndex;
    } else {
      if (endIndex < this.startIndex) {
        throw new IllegalArgumentException(
            "end index should be >= start index");
      }
      this.endIndex = (int) min(endIndex, maxIndex);
    }
    this.coder = coder;
  }

  @Override
  public SourceIterator<T> iterator() throws IOException {
    return new InMemorySourceIterator();
  }

  /**
   * A SourceIterator that yields an in-memory list of elements.
   */
  class InMemorySourceIterator extends AbstractSourceIterator<T> {
    int index;
    int endPosition;

    public InMemorySourceIterator() {
      index = startIndex;
      endPosition = endIndex;
    }

    @Override
    public boolean hasNext() {
      return index < endPosition;
    }

    @Override
    public T next() throws IOException {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      String encodedElementString = encodedElements.get(index++);
      // TODO: Replace with the real encoding used by the
      // front end, when we know what it is.
      byte[] encodedElement =
          StringUtils.jsonStringToByteArray(encodedElementString);
      notifyElementRead(encodedElement.length);
      return CoderUtils.decodeFromByteArray(coder, encodedElement);
    }

    @Override
    public Progress getProgress() {
      // Currently we assume that only a record index position is reported as
      // current progress. Source writer can override this method to update
      // other metrics, e.g. completion percentage or remaining time.
      com.google.api.services.dataflow.model.Position currentPosition =
          new com.google.api.services.dataflow.model.Position();
      currentPosition.setRecordIndex((long) index);

      ApproximateProgress progress = new ApproximateProgress();
      progress.setPosition(currentPosition);

      return cloudProgressToSourceProgress(progress);
    }

    @Override
    public Position updateStopPosition(Progress proposedStopPosition) {
      checkNotNull(proposedStopPosition);

      // Currently we only support stop position in record index of
      // an API Position in InMemorySource. If stop position in other types is
      // proposed, the end position in iterator will not be updated,
      // and return null.
      com.google.api.services.dataflow.model.Position stopPosition =
          sourceProgressToCloudProgress(proposedStopPosition).getPosition();
      if (stopPosition == null) {
        LOG.warn(
            "A stop position other than a Dataflow API Position is not currently supported.");
        return null;
      }

      Long recordIndex = stopPosition.getRecordIndex();
      if (recordIndex == null) {
        LOG.warn(
            "A stop position other than record index is not supported in InMemorySource.");
        return null;
      }
      if (recordIndex <= index || recordIndex >= endPosition) {
        // Proposed stop position is not after the current position or proposed
        // stop position is after the current stop (end) position: No stop
        // position update.
        return null;
      }

      this.endPosition = recordIndex.intValue();
      return cloudPositionToSourcePosition(stopPosition);
    }
  }
}
