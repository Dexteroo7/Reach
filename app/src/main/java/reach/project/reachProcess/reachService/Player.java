package reach.project.reachProcess.reachService;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.reachProcess.decoder.BitStream;
import reach.project.reachProcess.decoder.BitStreamException;
import reach.project.reachProcess.decoder.Decoder;
import reach.project.reachProcess.decoder.DecoderException;
import reach.project.reachProcess.decoder.Header;
import reach.project.reachProcess.decoder.SampleBuffer;
import reach.project.utils.CustomThreadFactoryBuilder;

/**
 * Created by Dexter on 22-06-2015.
 */
public class Player {

    //Single threaded executor to avoid fuck ups
    private final ExecutorService decoderService = Executors.newSingleThreadExecutor(
            new CustomThreadFactoryBuilder()
                    .setDaemon(false)
                    .setNamePrefix("reach_decoder")
                    .setPriority(Thread.MAX_PRIORITY)
                    .build());

    private final AtomicBoolean stopDecoding = new AtomicBoolean(true), pauseDecoding = new AtomicBoolean(false);
    private final DecoderHandler handlerInterface;
    private WhichPlayer whichPlayer = WhichPlayer.MediaPlayer;
    private AudioTrack audioTrack;
    private MediaPlayer mediaPlayer;
    private Future decodeFuture;

    public Player(DecoderHandler handlerInterface) {
        this.handlerInterface = handlerInterface;
    }

    public boolean isNull() {
        return (mediaPlayer == null && audioTrack == null) || whichPlayer == null;
    }

    public boolean isPlaying() {
        return (audioTrack != null && whichPlayer == WhichPlayer.AudioTrack && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) ||
                (mediaPlayer != null && whichPlayer == WhichPlayer.MediaPlayer && mediaPlayer.isPlaying());
    }

    public void reset() throws InterruptedException {

        stopDecoding.set(true);
        if (decodeFuture != null) {
            decodeFuture.cancel(true);
            try {
                decodeFuture.get();
            } catch (ExecutionException | CancellationException ignored) {
            }
        }
        if (audioTrack != null) {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack.stop();
        }
        if (mediaPlayer != null)
            mediaPlayer.reset();
    }

    public int getCurrentPosition() {

        if (whichPlayer == WhichPlayer.MediaPlayer)
            return mediaPlayer.getCurrentPosition() / 1000;
        else
            return (audioTrack.getPlaybackHeadPosition() / audioTrack.getSampleRate());
    }

    /**
     * Pauses which ever player is running, no checks, much lazy
     */
    public void pause() {

        if (audioTrack != null && whichPlayer == WhichPlayer.AudioTrack) {
            audioTrack.pause();
            pauseDecoding.set(true);
        }
        if (mediaPlayer != null && whichPlayer == WhichPlayer.MediaPlayer)
            mediaPlayer.pause();
    }

    public void start() {

        if (whichPlayer == WhichPlayer.MediaPlayer)
            mediaPlayer.start();
        else {
            audioTrack.play();
            pauseDecoding.set(false);
        }
    }

    public void setVolume(float duck_volume) {
        if (mediaPlayer != null)
            mediaPlayer.setVolume(duck_volume, duck_volume);
        if (audioTrack == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            audioTrackVolumeNew(duck_volume);
        else
            audioTrackVolumeOld(duck_volume);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void audioTrackVolumeNew(float duck_volume) {
        audioTrack.setVolume(duck_volume);
    }

    @SuppressWarnings("deprecation")
    private void audioTrackVolumeOld(float duck_volume) {
        audioTrack.setStereoVolume(duck_volume, duck_volume);
    }

    public void seekTo(int i) throws UnsupportedOperationException {

        Log.i("Downloader", "Seeking to " + i);
        if (whichPlayer == WhichPlayer.MediaPlayer && mediaPlayer != null)
            mediaPlayer.seekTo(i);
        else throw new UnsupportedOperationException("Seek not allowed in AudioTrack yet !");
    }

    /**
     * @param data the bytes to be fed
     * @return true : successfully fed the data
     * false : some error occurred
     */
    public boolean feedData(short[] data) {

        final int result = audioTrack.write(data, 0, data.length);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return resultAnalyzeNew(result);
        else
            return resultAnalyzeOld(result);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean resultAnalyzeNew(int result) {
        return !(result == AudioTrack.ERROR_INVALID_OPERATION || //-3
                result == AudioTrack.ERROR_BAD_VALUE || //-2
                result == AudioTrack.ERROR || //-1
                result == AudioManager.ERROR_DEAD_OBJECT); //-6
    }

    private boolean resultAnalyzeOld(int result) {
        return !(result == AudioTrack.ERROR_INVALID_OPERATION || //-3
                result == AudioTrack.ERROR_BAD_VALUE || //-2
                result == AudioTrack.ERROR); //-1
    }

    public void cleanUp() {

        stopDecoding.set(true);
        if (decodeFuture != null)
            decodeFuture.cancel(true);
        if (audioTrack != null)
            audioTrack.release();
        if (mediaPlayer != null)
            mediaPlayer.release();
        decoderService.shutdownNow();
    }

    protected int createMediaPlayerIfNeeded(MediaPlayer.OnCompletionListener completionListener,
                                            MediaPlayer.OnErrorListener onErrorListener,
                                            String path) throws IOException, InterruptedException {

        reset();
        whichPlayer = WhichPlayer.MediaPlayer;

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(completionListener);
            mediaPlayer.setOnErrorListener(onErrorListener);
        }
        mediaPlayer.setDataSource(path);
        mediaPlayer.prepare();
        return mediaPlayer.getDuration();
    }

    protected int createAudioTrackIfNeeded(Optional<String> path, long contentLength) throws IOException, InterruptedException {

        reset();
        whichPlayer = WhichPlayer.AudioTrack;

        if (!path.isPresent() || TextUtils.isEmpty(path.get()) || path.get().equals("hello_world"))
            throw new IOException("Given path is invalid");

        final InputStream source = new FileInputStream(path.get());
        final BitStream bitStream = new BitStream(source);
        final Header frameHeader;

        final Future<Header> getHeader = decoderService.submit(new Callable<Header>() {
            @Override
            public Header call() throws BitStreamException, InterruptedException {
                //safety check for mp3 frame size
                while (handlerInterface.getProcessed() < 4096) {
                    Log.i("Downloader", "Waiting for minimum buffer");
                    Thread.sleep(4000L);
                }
                return bitStream.readFrame();
            }
        });

        try {
            //caller should handle InterruptedException
            frameHeader = getHeader.get(12, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {

            e.printStackTrace();
            try {
                bitStream.close(); //closes source
            } catch (BitStreamException ignored) {
            }
            throw new IOException("Probably corrupt file : header fetch timed out, " + e.getLocalizedMessage());
        } finally {
            getHeader.cancel(true);
        }

        if (frameHeader == null)
            throw new IOException("Probably corrupt file : null frameHeader");

        final int duration = (int) frameHeader.total_ms((int) contentLength);
        final int sampleFrequency = frameHeader.frequency();
        final boolean mono = frameHeader.mode() == Header.SINGLE_CHANNEL;
        final int mode = mono ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        final boolean createNew = audioTrack == null || audioTrack.getSampleRate() != sampleFrequency || audioTrack.getChannelCount() != (mono ? 1 : 2);

        Log.i("Downloader", "Maximum MS " + duration);
        Log.i("Downloader", "Sample Frequency " + sampleFrequency);
        Log.i("Downloader", "Mono " + mono);
        Log.i("Downloader", "Create New " + createNew);

        if (createNew) {
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleFrequency,
                    mode,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sampleFrequency * 2,
                    AudioTrack.MODE_STREAM);
        }

        decodeFuture = decoderService.submit(new Decode(bitStream, frameHeader, contentLength, mono, sampleFrequency * 2));

        //half fill with zeroes
        final byte[] zeroes = new byte[sampleFrequency];
        Arrays.fill(zeroes, 0, zeroes.length, (byte) 0);
        audioTrack.write(zeroes, 0, zeroes.length);

        return duration;
    }

    //which player is playing
    private enum WhichPlayer {

        AudioTrack, //we are streaming
        MediaPlayer //static playBack (seek-able)
    }

    ///////////////////////////////////////
    public interface DecoderHandler {
        long getProcessed();

        void updateSecondaryProgress(short progress);

        void onCompletion(MediaPlayer player);
    }

    private final class Decode implements Runnable {

        private final short[] buffer;
        private final BitStream bitStream;
        private final long contentLength;
        private final boolean mono;
        private Header frameHeader;
        private int limit = 0;

        public Decode(BitStream bitStream, Header frameHeader, long contentLength, boolean mono, int bufferSize) {
            this.bitStream = bitStream;
            this.contentLength = contentLength;
            this.frameHeader = frameHeader;
            this.mono = mono;
            this.buffer = new short[bufferSize];
        }

        private void pause() {
            try {
                Thread.sleep(4000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopDecoding.set(true);
            }
        }

        private void buffer(short[] data) {

            final int effectiveLength = mono ? data.length / 2 : data.length;
            System.arraycopy(data, 0, buffer, limit, effectiveLength);
            limit += effectiveLength;

            if (buffer.length - limit < effectiveLength)
                flush();
        }

        private boolean flush() {
            if (limit > 0) {
                Log.i("Downloader", "Flushing " + limit);
                audioTrack.write(buffer, 0, limit);
                Log.i("Downloader", "Flushed");
                limit = 0;
                return true;
            }
            return false;
        }

        @Override
        public void run() {

            pauseDecoding.set(false);
            stopDecoding.set(false);
            final Decoder decoder = new Decoder();
            long transferred = 0, count = 0;
            short lastProgress = 0;

            while (!stopDecoding.get()) {

                if (pauseDecoding.get()) {
                    Log.i("Downloader", "PAUSING DECODER -> user");
                    pause();
                    continue;
                }

                transferred += frameHeader.framesize;
                //Log.i("Downloader", frameHeader.mode() + " " + frameHeader.frequency());

                short[] toFeed = null;
                try {
                    toFeed = ((SampleBuffer) decoder.decodeFrame(frameHeader, bitStream)).getBuffer();
                } catch (DecoderException ignored) {
                    //ignore frame
                } finally {

                    if (toFeed != null && toFeed.length > 0)
                        buffer(toFeed);
                    else
                        Log.i("Downloader", "could not feed");
                    bitStream.closeFrame();
                }

                while (transferred + 4096 >= count && count != contentLength && !stopDecoding.get()) {

                    count = handlerInterface.getProcessed();
                    final short progress = (short) ((count * 100) / contentLength);
                    if (progress > lastProgress)
                        handlerInterface.updateSecondaryProgress(progress);
                    if (transferred + 4096 >= count && count != contentLength) {
                        Log.i("Downloader", "PAUSING DECODER");
                        pause();
                    }
                    lastProgress = progress;
                }

                try {
                    frameHeader = bitStream.readFrame();
                } catch (BitStreamException e) {
                    e.printStackTrace();
                    frameHeader = null;
                }

                if (frameHeader == null) {

                    if (flush())
                        try {
                            Thread.sleep(500L); //wait for the flush to be played
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }

                    handlerInterface.onCompletion(null);
                    Log.i("Downloader", "Frame header null BREAKING !");
                    break;
                }
            }

            try {
                bitStream.close();
            } catch (BitStreamException ignored) {
            }
        }
    }
}