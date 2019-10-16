import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author Gavin
 * 2019/8/28 17:19
 */
public class MetricServer {

    private InetSocketAddress localAddress;
    private Channel channel;

    public static void main(String[] args) throws Exception {
        new MetricServer(10010);
    }

    public MetricServer(int port) throws Exception {
        ServerNetProtocol serverNetProtocol = new ServerNetProtocol(this);
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.group(group);
        bootstrap.option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline cp = ch.pipeline();
                cp.addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        DatagramPacket datagramPacket = (DatagramPacket) msg;
                        serverNetProtocol.addData(datagramPacket);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    }
                });
            }
        });
        ChannelFuture channelFuture = bootstrap.bind(port).await();
        channel = channelFuture.channel();
        localAddress = (InetSocketAddress) channel.localAddress();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                group.shutdownGracefully();
            }
        }));
    }

    public void output(ByteBuf msg, InetSocketAddress address) {
        System.out.println("返回的ACK:" + msg.getCharSequence(0, 32, StandardCharsets.UTF_8) + ","
                + msg.getInt(32));
        DatagramPacket ack = new DatagramPacket(msg, address, this.localAddress);
        channel.writeAndFlush(ack);
    }
}
