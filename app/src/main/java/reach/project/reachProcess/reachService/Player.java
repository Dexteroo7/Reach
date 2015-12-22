package reach.project.reachProcess.reachService;


import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.FrameworkSampleSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.util.PlayerControl;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.core.StaticData;
import reach.project.reachProcess.decoder.BitStream;
import reach.project.reachProcess.decoder.BitStreamException;
import reach.project.reachProcess.decoder.Decoder;
import reach.project.reachProcess.decoder.DecoderException;
import reach.project.reachProcess.decoder.Header;
import reach.project.reachProcess.decoder.SampleBuffer;

/**
 * Created by Dexter on 22-06-2015.
 */
public class Player {

    //which player is playing
    private enum WhichPlayer {

        AudioTrack, //we are streaming
        MediaPlayer, //static playBack (seek-able)
        Exoplayer
    }

    //Single threaded executor to avoid fuck ups
    private final ExecutorService decoderService = Executors.unconfigurableExecutorService(
            Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
                    .setDaemon(false)
                    .setNameFormat("reach_decoder")
                    .setPriority(Thread.MAX_PRIORITY)
                    .setUncaughtExceptionHandler((thread, ex) -> {

                        //ex.getLocalizedMessage()
                        //TODO track
                    })
                    .build()));

    private final AtomicBoolean stopDecoding = new AtomicBoolean(true), pauseDecoding = new AtomicBoolean(false);
    private final DecoderHandler handlerInterface;
    private TrackRenderer audio;

    private WhichPlayer whichPlayer = WhichPlayer.MediaPlayer;
    private AudioTrack audioTrack;
    private MediaPlayer mediaPlayer;
    private ExoPlayer exoplayer;

    private Future decodeFuture;
    private Future pipeFuture;
    private PlayerSource playerSource;

    public Player(DecoderHandler handlerInterface) {
        this.handlerInterface = handlerInterface;
    }

    public boolean isNull()
    {
        return (mediaPlayer == null && exoplayer==null && audioTrack == null) || whichPlayer == null;
    }

    public boolean isPlaying()
    {
        try
        {
            PlayerControl pl=new PlayerControl(exoplayer);
            boolean IS_PLAYING_EXOPLAYER=(exoplayer!=null && whichPlayer==WhichPlayer.Exoplayer && pl.isPlaying());
            boolean IS_PLAYING_MEDIA_PLAYER=(mediaPlayer != null && whichPlayer == WhichPlayer.MediaPlayer && mediaPlayer.isPlaying());
            boolean IS_PLAYING_AUDIO_TRACK=(whichPlayer == WhichPlayer.AudioTrack && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
            return IS_PLAYING_AUDIO_TRACK || IS_PLAYING_EXOPLAYER || IS_PLAYING_MEDIA_PLAYER;
        }
        catch (IllegalStateException ignored)
        {

        }

        return false;
    }

    public void reset() throws InterruptedException {

        stopDecoding.set(true);
        Log.i("Downloader", "Resetting player");
        if (playerSource != null)
            playerSource.close();
//        Log.i("Downloader", "Cancelling decoder");
        if (decodeFuture != null) {
            decodeFuture.cancel(true);
            try
            {
                decodeFuture.get();
            } catch (ExecutionException | CancellationException ignored) {
            }
        }
//        Log.i("Downloader", "Cancelling pipe");
        if (pipeFuture != null) {
            pipeFuture.cancel(true);
            try {
                pipeFuture.get();
            } catch (ExecutionException | CancellationException ignored) {
            }
        }
//        Log.i("Downloader", "Resetting audio track");
        if (audioTrack != null) {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack.stop();
        }
//        Log.i("Downloader", "Resetting mediaPlayer");
        if (mediaPlayer != null)
            mediaPlayer.reset();
        if(exoplayer!=null)
            exoplayer.stop();
        Log.i("Downloader", "Reset complete");
    }

    public int getCurrentPosition() {

        if (whichPlayer == WhichPlayer.MediaPlayer)
            return mediaPlayer.getCurrentPosition() / 1000;
        else if(whichPlayer==WhichPlayer.Exoplayer)
            return (int)(exoplayer.getCurrentPosition()/1000);
        else
            return (audioTrack.getPlaybackHeadPosition() / audioTrack.getSampleRate());
    }

    /**
     * Pauses which ever player is running, no checks, much lazy
     */
    public void pause()
    {
        if(exoplayer!=null && whichPlayer==WhichPlayer.Exoplayer)
        {
            exoplayer.setPlayWhenReady(false);
        }

        if (audioTrack != null && whichPlayer == WhichPlayer.AudioTrack)
        {
            audioTrack.pause();
            pauseDecoding.set(true);
        }
        if (mediaPlayer != null && whichPlayer == WhichPlayer.MediaPlayer)
        {
            mediaPlayer.pause();
        }


    }

    public void start()
    {

        if (whichPlayer == WhichPlayer.MediaPlayer)
        {
            mediaPlayer.start();
        }
        else if(whichPlayer==WhichPlayer.Exoplayer)
        {
            exoplayer.setPlayWhenReady(true);
        }
        else
        {
            audioTrack.play();
            pauseDecoding.set(false);
        }
    }

    public void setVolume(float duck_volume)
    {
        if(exoplayer!=null)
            exoplayer.sendMessage(audio,MediaCodecAudioTrackRenderer.MSG_SET_VOLUME,duck_volume);
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
        else if(whichPlayer==WhichPlayer.Exoplayer && exoplayer!=null) {
            int buffered=exoplayer.getBufferedPercentage();
            if(i<buffered)
                exoplayer.seekTo(i);
            else
                exoplayer.seekTo(exoplayer.getBufferedPosition());
        }
        else throw new UnsupportedOperationException("Seek not allowed in AudioTrack yet !");
    }

//    /**
//     * @param data the bytes to be fed
//     * @return true : successfully fed the data
//     * false : some error occurred
//     */
//    public boolean feedData(short[] data) {
//
//        final int result = audioTrack.write(data, 0, data.length);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//            return resultAnalyzeNew(result);
//        else
//            return resultAnalyzeOld(result);
//    }
//
//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    private boolean resultAnalyzeNew(int result) {
//        return !(result == AudioTrack.ERROR_INVALID_OPERATION || //-3
//                result == AudioTrack.ERROR_BAD_VALUE || //-2
//                result == AudioTrack.ERROR || //-1
//                result == AudioManager.ERROR_DEAD_OBJECT); //-6
//    }
//
//    private boolean resultAnalyzeOld(int result) {
//        return !(result == AudioTrack.ERROR_INVALID_OPERATION || //-3
//                result == AudioTrack.ERROR_BAD_VALUE || //-2
//                result == AudioTrack.ERROR); //-1
//    }

    public void cleanUp() {

        Log.i("Downloader", "Cleaning up player");

        stopDecoding.set(true);

        if (playerSource != null)
            playerSource.close();

//        Log.i("Downloader", "Closing decoder service");

        if (decodeFuture != null)
            decodeFuture.cancel(true);

//        Log.i("Downloader", "Closing pipe service");

        if (pipeFuture != null)
            pipeFuture.cancel(true);

//        Log.i("Downloader", "Releasing audio track");

        if (audioTrack != null)
            audioTrack.release();

//        Log.i("Downloader", "Releasing media player");

        if (mediaPlayer != null)
            mediaPlayer.release();

        if(exoplayer!=null)
            exoplayer.seekTo(0);

//        Log.i("Downloader", "shutting down decoder service");
        decoderService.shutdownNow();
        Log.i("Downloader", "decoder service shutdown !");
    }

    protected int createMediaPlayerIfNeeded(MediaPlayer.OnCompletionListener completionListener,
                                            MediaPlayer.OnErrorListener onErrorListener,
                                            String path) throws IOException, InterruptedException {
        reset(); //throws InterruptedException
        whichPlayer = WhichPlayer.MediaPlayer;
        if (mediaPlayer == null)
        {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(completionListener);
            mediaPlayer.setOnErrorListener(onErrorListener);
        }
        mediaPlayer.setDataSource(path);
        mediaPlayer.prepare();
        return mediaPlayer.getDuration();

    }
    protected long createExoPlayerIfNeeded(MediaPlayer.OnCompletionListener completionListener,
                                            MediaPlayer.OnErrorListener onErrorListener,
                                            String path) throws IOException, InterruptedException {
        reset(); //throws InterruptedException

        whichPlayer=WhichPlayer.Exoplayer;
        if (exoplayer == null)
        {
            exoplayer=ProcessManager.exoplayer;
        }
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        FileDescriptor fd = inputStream.getFD();
        SampleSource sampleSource = new FrameworkSampleSource(fd, 0, file.length());
        audio = new MediaCodecAudioTrackRenderer(sampleSource, null, true);
        exoplayer.prepare(audio);
        exoplayer.seekTo(0);
        return exoplayer.getDuration();
    }

    protected long createStreamingExoPlayerIfNeeded(Optional<String> path,final long contentLength) throws IOException, InterruptedException
    {
        reset();
        whichPlayer=WhichPlayer.Exoplayer;

        if (!path.isPresent() || TextUtils.isEmpty(path.get()) || path.get().equals("hello_world"))
            throw new IOException("Given path is invalid");

        if(exoplayer == null)
        {
            exoplayer=ProcessManager.exoplayer;
        }
        //create source
        playerSource = new PlayerSource(
                handlerInterface,
                path.get(),
                contentLength);

        String pa=path.get();
        SampleSource sampleSource = new FrameworkSampleSource(fd, 0, file.length());
        audio = new MediaCodecAudioTrackRenderer(sampleSource, null, true);
        exoplayer.prepare(audio);
        exoplayer.seekTo(0);
        Log.i("Exoplayer bufffer","Done");
        return exoplayer.getDuration();

    }

    protected long createAudioTrackIfNeeded(Optional<String> path, final long contentLength) throws IOException, InterruptedException {

//        Log.i("Downloader", "Creating audio track");
        reset(); //throws InterruptedException
        whichPlayer = WhichPlayer.AudioTrack;

        if (!path.isPresent() || TextUtils.isEmpty(path.get()) || path.get().equals("hello_world"))
            throw new IOException("Given path is invalid");

        //create source
        playerSource = new PlayerSource(
                handlerInterface,
                path.get(),
                contentLength);

        //start thread
        pipeFuture = decoderService.submit(playerSource);

        final BitStream bitStream = new BitStream(playerSource.getSource(), StaticData.PLAYER_BUFFER_DEFAULT);
        final Header frameHeader;

        //do like this to prevent blocking
        final Future<Header> getHeader = decoderService.submit(bitStream::readFrame);

        try {
            //caller should handle InterruptedException
            frameHeader = getHeader.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {

            try {
                bitStream.close();
            } catch (BitStreamException ignored) {
            }
//            playerSource.close();
//            pipeFuture.cancel(true);
            e.printStackTrace();
            throw new IOException("Probably corrupt file : header fetch timed out, " + Throwables.getStackTraceAsString(e));
        } finally {
            getHeader.cancel(true);
        }

        if (frameHeader == null)
            throw new IOException("Probably corrupt file : null frameHeader");

        final long duration = frameHeader.total_ms(contentLength);
        final int sampleFrequency = frameHeader.frequency();
        final boolean mono = frameHeader.mode() == Header.SINGLE_CHANNEL;
        final int mode = mono ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        final boolean createNew = audioTrack == null || audioTrack.getSampleRate() != sampleFrequency || audioTrack.getChannelCount() != (mono ? 1 : 2);

        Log.i("Downloader", "Maximum MS " + duration);
        Log.i("Downloader", "MS Per frame" + frameHeader.ms_per_frame() + " " + contentLength);
        Log.i("Downloader", "Sample Frequency " + sampleFrequency);
        Log.i("Downloader", "Mono " + mono);
        Log.i("Downloader", "Create New " + createNew);
        final int bufferSize = sampleFrequency * 2;

        if (createNew) {
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleFrequency,
                    mode,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM);
        }

        //half fill with zeroes
        final byte[] zeroes = new byte[bufferSize / 2];
        Arrays.fill(zeroes, 0, zeroes.length, (byte) 0);
        audioTrack.write(zeroes, 0, zeroes.length);

        decodeFuture = decoderService.submit(new Decode(bitStream, frameHeader, mono, bufferSize / 2));

        return duration;
    }

    private final class Decode implements Runnable {

        private final short[] buffer;
        private final BitStream bitStream;
        private final boolean mono;
        private Header frameHeader;
        private int limit = 0;

        public Decode(BitStream bitStream, Header frameHeader, boolean mono, int bufferSize) {
            this.bitStream = bitStream;
            this.frameHeader = frameHeader;
            this.mono = mono;
            this.buffer = new short[bufferSize];
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
                audioTrack.write(buffer, 0, limit);
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

            while (!stopDecoding.get()) {

                if (pauseDecoding.get())
                    try {
                        Thread.sleep(500L);
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        stopDecoding.set(true);
                        break;
                    }
                //Log.i("Downloader", frameHeader.mode() + " " + frameHeader.frequency());
                short[] toFeed = null;
                try {
                    toFeed = ((SampleBuffer) decoder.decodeFrame(frameHeader, bitStream)).getBuffer();
                } catch (DecoderException ignored) {
                    //ignore frame
                } catch (Exception e) {
                    e.printStackTrace();
                    break; //quit !!
                } finally {

                    if (toFeed != null && toFeed.length > 0)
                        buffer(toFeed);
                    else
                        Log.i("Downloader", "could not feed");
                    bitStream.closeFrame();
                }

                try {
                    frameHeader = bitStream.readFrame();
                } catch (Exception e) {
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

            try
            {
                bitStream.close();
            }
            catch (BitStreamException ignored) {
            }
            finally
            {
                playerSource.close();
                pipeFuture.cancel(true);
            }
        }
    }

    ///////////////////////////////////////
    public interface DecoderHandler {
        long getProcessed();

        void updateSecondaryProgress(short progress);

        void onCompletion(MediaPlayer player);

        Context getContext();
    }
}