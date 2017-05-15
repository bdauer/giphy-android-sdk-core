/*
 * Created by Bogdan Tirca on 4/26/17.
 * Copyright (c) 2017 Giphy Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.giphy.sdk.core;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.giphy.sdk.core.models.Media;
import com.giphy.sdk.core.models.deserializers.BooleanDeserializer;
import com.giphy.sdk.core.models.deserializers.DateDeserializer;
import com.giphy.sdk.core.models.deserializers.IntDeserializer;
import com.giphy.sdk.core.models.deserializers.MainAdapterFactory;
import com.giphy.sdk.core.models.enums.MediaType;
import com.giphy.sdk.core.network.api.CompletionHandler;
import com.giphy.sdk.core.network.api.GPHApiClient;
import com.giphy.sdk.core.network.engine.NetworkSession;
import com.giphy.sdk.core.network.response.GenericResponse;
import com.giphy.sdk.core.network.response.ListMediaResponse;
import com.giphy.sdk.core.threading.ApiTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpIntegrationTest {
    GPHApiClient imp;

    @Before
    public void setUp() throws Exception {
        imp = new GPHApiClient("dc6zaTOxFJmzC", new OkHttptNetworkSession());
    }

    /**
     * Test if trending without params returns 25 gifs and not exception.
     * @throws Exception
     */
    @Test
    public void testTrending() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        imp.trending(MediaType.gif, null, null, null, new CompletionHandler<ListMediaResponse>() {
            @Override
            public void onComplete(ListMediaResponse result, Throwable e) {
                Assert.assertNull(e);
                Assert.assertNotNull(result);
                Assert.assertNotNull(result.getData());
                Assert.assertTrue(result.getData().size() == 25);

                for (Media media : result.getData()) {
                    Assert.assertNotNull(media.getId());
                    Assert.assertNotNull(media.getImages());
                }
                lock.countDown();
            }
        });
        lock.await(2000, TimeUnit.MILLISECONDS);
    }

    /**
     * Test if search without params returns 25 gifs and not exception.
     *
     * @throws Exception
     */
    @Test
    public void testSearch() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        imp.search("hack", MediaType.gif, null, null, null, null, new CompletionHandler<ListMediaResponse>() {
            @Override
            public void onComplete(ListMediaResponse result, Throwable e) {
                Assert.assertNull(e);
                Assert.assertNotNull(result);
                Assert.assertNotNull(result.getData());
                Assert.assertTrue(result.getData().size() == 25);

                for (Media media : result.getData()) {
                    Assert.assertNotNull(media.getId());
                    Assert.assertNotNull(media.getImages());
                    Assert.assertNotNull(media.getType());
                }

                lock.countDown();
            }
        });
        lock.await(2000, TimeUnit.MILLISECONDS);
    }

    static class OkHttptNetworkSession implements NetworkSession {
        private static final Gson GSON_INSTANCE = new GsonBuilder()
                .registerTypeHierarchyAdapter(Date.class, new DateDeserializer())
                .registerTypeHierarchyAdapter(boolean.class, new BooleanDeserializer())
                .registerTypeHierarchyAdapter(int.class, new IntDeserializer())
                .registerTypeAdapterFactory(new MainAdapterFactory())
                .create();

        @Override
        public <T extends GenericResponse> ApiTask<T> queryStringConnection(@NonNull final Uri serverUrl,
                                                                            @NonNull final String path,
                                                                            @NonNull final String method,
                                                                            @NonNull final Class<T> responseClass,
                                                                            @Nullable final Map<String, String> queryStrings,
                                                                            @Nullable final Map<String, String> headers) {
            return new ApiTask<>(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    final Uri.Builder uriBuilder = serverUrl.buildUpon().appendEncodedPath(path);

                    if (queryStrings != null) {
                        for (Map.Entry<String, String> query : queryStrings.entrySet()) {
                            uriBuilder.appendQueryParameter(query.getKey(), query.getValue());
                        }
                    }

                    final URL url = new URL(uriBuilder.build().toString());

                    final Request.Builder requestBuilder = new Request.Builder()
                            .url(url);

                    if (headers != null) {
                        for (Map.Entry<String, String> header : headers.entrySet()) {
                            requestBuilder.addHeader(header.getKey(), header.getValue());
                        }
                    }

                    final Request request = requestBuilder.build();

                    final OkHttpClient client = new OkHttpClient();
                    final Response response = client.newCall(request).execute();
                    // Deserialize HTTP response to concrete type.

                    final ResponseBody body = response.body();
                    return GSON_INSTANCE.fromJson(body.string(), responseClass);
                }
            });
        }
    }
}
