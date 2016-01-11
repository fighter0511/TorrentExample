/*
 * Copyright 2012 Sebastian Annies, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package frostwire.mp4;

import java.util.Arrays;

/**
 * This <code>FragmentIntersectionFinder</code> cuts the input movie in 2 second
 * snippets.
 */
public class TimeBasedFragmenter implements Fragmenter {
    private double fragmentLength = 2;

    public TimeBasedFragmenter(double fragmentLength) {
        this.fragmentLength = fragmentLength;
    }

    /**
     * {@inheritDoc}
     */
    public long[] sampleNumbers(Track track) {
        long[] segmentStartSamples = new long[]{1};
        long[] sampleDurations = track.getSampleDurations();
        long[] syncSamples = track.getSyncSamples();
        long timescale = track.getTrackMetaData().getTimescale();
        double time = 0;
        for (int i = 0; i < sampleDurations.length; i++) {
            time += (double) sampleDurations[i] / timescale;
            if (time >= fragmentLength &&
                    (syncSamples == null || Arrays.binarySearch(syncSamples, i + 1) >= 0)) {
                if (i > 0) {
                    segmentStartSamples = Mp4Arrays.copyOfAndAppend(segmentStartSamples, i + 1);
                }
                time = 0;
            }
        }
        return segmentStartSamples;
    }

    public static void main(String[] args) {
        DefaultMp4Builder b = new DefaultMp4Builder();
        b.setFragmenter(new TimeBasedFragmenter(0.5));
    }
}
