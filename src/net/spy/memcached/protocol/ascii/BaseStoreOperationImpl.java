/**
 * Copyright (C) 2006-2009 Dustin Sallings
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;

/**
 * Base class for ascii store operations (add, set, replace, append, prepend).
 */
abstract class BaseStoreOperationImpl extends OperationImpl {

  private static final int OVERHEAD = 32;
  private static final OperationStatus STORED = new OperationStatus(true,
      "STORED", StatusCode.SUCCESS);
  protected final String type;
  protected final String key;
  protected final int cost;
  protected final int flags;
  protected final int exp;
  protected final byte[] data;
  protected StringBuffer filewriter;
  protected HashMap<String,Integer> lambda;

  public BaseStoreOperationImpl(String t, String k, HashMap<String,Integer> lambda, int c, int f, int e, byte[] d,
      OperationCallback cb, StringBuffer filewriter) {
    super(cb);
    type = t;
    key = k;
    cost = c;
    flags = f;
    exp = e;
    data = d;
    this.lambda = lambda;
    this.filewriter = filewriter;
  }

  @Override
  public void handleLine(String line) {
	
	int lmb = Integer.parseInt(line.substring(line.length() - 1, line.length()));
	line = line.substring(0, line.length() - 1);
	System.out.println("insert " + key + " " + line + " " + lmb);
	lambda.put(key, lmb);
    assert getState() == OperationState.READING : "Read ``" + line
        + "'' when in " + getState() + " state";
    System.out.println(line);
    filewriter.append(" ");
    filewriter.append((line.equals("STORED") ? line : "NOT STORED") + "\n");
    getCallback().receivedStatus(matchStatus(line, STORED));
    transitionState(OperationState.COMPLETE);
    
  }

  @Override
  public void initialize() {
    ByteBuffer bb = ByteBuffer.allocate(data.length
        + KeyUtil.getKeyBytes(key).length + OVERHEAD);
    setArguments(bb, type, key, cost, flags, exp, data.length);
    assert bb.remaining() >= data.length + 2 : "Not enough room in buffer,"
        + " need another " + (2 + data.length - bb.remaining());
    bb.put(data);
    bb.put(CRLF);
    bb.flip();
    setBuffer(bb);
  }

  @Override
  protected void wasCancelled() {
    // XXX: Replace this comment with why I did this
    getCallback().receivedStatus(CANCELLED);
  }

  public Collection<String> getKeys() {
    return Collections.singleton(key);
  }
  
  public int getCost() {
	  return cost;
  }
  public HashMap<String,Integer> gethashmap(){
	  return lambda;
  }

  public int getFlags() {
    return flags;
  }

  public int getExpiration() {
    return exp;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public String toString() {
    return "Cmd: " + type + " Key: " +  key + " Cost "+ cost + " Flags: " + flags + " Exp: "
      + exp + " Data Length: " + data.length;
  }
}
