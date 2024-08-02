package com.czhj.sdk.common.models;

import android.os.Parcelable;

import com.czhj.wire.AndroidMessage;
import com.czhj.wire.FieldEncoding;
import com.czhj.wire.Message;
import com.czhj.wire.ProtoAdapter;
import com.czhj.wire.ProtoReader;
import com.czhj.wire.ProtoWriter;
import com.czhj.wire.WireField;
import com.czhj.wire.internal.Internal;
import com.czhj.wire.okio.ByteString;

import java.io.IOException;

public final class StrategyReq extends AndroidMessage<StrategyReq, StrategyReq.Builder> {
  public static final ProtoAdapter<StrategyReq> ADAPTER = new ProtoAdapter_StrategyReq();

  public static final Parcelable.Creator<StrategyReq> CREATOR = AndroidMessage.newCreator(ADAPTER);

  private static final long serialVersionUID = 0L;

  public static final Boolean DEFAULT_STRATEGY_INIT = false;

  public static final String DEFAULT_LAST_STRATEGY_ID = "";

  /**
   * 略是否初始化请求标记
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  public final Boolean strategy_init;

  /**
   * 上一次的下发的策略分组id 按广告位区分
   */
  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String last_strategy_id;

  public StrategyReq(Boolean strategy_init, String last_strategy_id) {
    this(strategy_init, last_strategy_id, ByteString.EMPTY);
  }

  public StrategyReq(Boolean strategy_init, String last_strategy_id, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.strategy_init = strategy_init;
    this.last_strategy_id = last_strategy_id;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.strategy_init = strategy_init;
    builder.last_strategy_id = last_strategy_id;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof StrategyReq)) return false;
    StrategyReq o = (StrategyReq) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(strategy_init, o.strategy_init)
        && Internal.equals(last_strategy_id, o.last_strategy_id);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (strategy_init != null ? strategy_init.hashCode() : 0);
      result = result * 37 + (last_strategy_id != null ? last_strategy_id.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (strategy_init != null) builder.append(", strategy_init=").append(strategy_init);
    if (last_strategy_id != null) builder.append(", last_strategy_id=").append(last_strategy_id);
    return builder.replace(0, 2, "StrategyReq{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<StrategyReq, Builder> {
    public Boolean strategy_init;

    public String last_strategy_id;

    public Builder() {
    }

    /**
     * 略是否初始化请求标记
     */
    public Builder strategy_init(Boolean strategy_init) {
      this.strategy_init = strategy_init;
      return this;
    }

    /**
     * 上一次的下发的策略分组id 按广告位区分
     */
    public Builder last_strategy_id(String last_strategy_id) {
      this.last_strategy_id = last_strategy_id;
      return this;
    }

    @Override
    public StrategyReq build() {
      return new StrategyReq(strategy_init, last_strategy_id, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_StrategyReq extends ProtoAdapter<StrategyReq> {
    public ProtoAdapter_StrategyReq() {
      super(FieldEncoding.LENGTH_DELIMITED, StrategyReq.class);
    }

    @Override
    public int encodedSize(StrategyReq value) {
      return ProtoAdapter.BOOL.encodedSizeWithTag(1, value.strategy_init)
          + ProtoAdapter.STRING.encodedSizeWithTag(2, value.last_strategy_id)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, StrategyReq value) throws IOException {
      ProtoAdapter.BOOL.encodeWithTag(writer, 1, value.strategy_init);
      ProtoAdapter.STRING.encodeWithTag(writer, 2, value.last_strategy_id);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public StrategyReq decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.strategy_init(ProtoAdapter.BOOL.decode(reader)); break;
          case 2: builder.last_strategy_id(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            FieldEncoding fieldEncoding = reader.peekFieldEncoding();
            Object value = fieldEncoding.rawProtoAdapter().decode(reader);
            builder.addUnknownField(tag, fieldEncoding, value);
          }
        }
      }
      reader.endMessage(token);
      return builder.build();
    }

    @Override
    public StrategyReq redact(StrategyReq value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
