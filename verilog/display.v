`timescale 1ns / 1ps

module Display (
	input  wire clk,
	input  wire clk_pix1,
	input  wire clk_pix5,
	input  wire clk30,
	input  wire rst_n,
	input  wire [23:0] audioL,
	input  wire [23:0] audioR,
	output wire hdmi_tx_clk_n,   // HDMI clock differential negative
	output wire hdmi_tx_clk_p,   // HDMI clock differential positive
	output wire [2:0] hdmi_tx_n, // Three HDMI channels differential negative
	output wire [2:0] hdmi_tx_p, // Three HDMI channels differential positive
	output wire [10:0] x,
	output wire [ 9:0] y,
	input  wire [ 7:0] red,
	input  wire [ 7:0] green,
	input  wire [ 7:0] blue
);

	// TMDS Encoding and Serialization
	wire tmds_ch0_serial, tmds_ch1_serial, tmds_ch2_serial, tmds_chc_serial;
	wire clk_audio;
	wire clk_audio_buf;

	reg [9:0] counter = 1'd0;
	always @(posedge clk30) begin
		counter <= counter == 10'd625 ? 1'd0 : counter + 1'd1;
	end

	assign clk_audio = clk30 && counter == 10'd625;

	BUFG audio_bufg (.I(clk_audio), .O(clk_audio_buf));

	hdmi #(
		.VIDEO_ID_CODE(4),
		.AUDIO_BIT_WIDTH(24),
		.VENDOR_NAME({"Heimskr", 8'b0}),
		.AUDIO_RATE(48000)
	) magic (
		.clk_pixel_x5(clk_pix5),
		.clk_pixel(clk_pix1),
		.clk_audio(clk_audio_buf),
		.reset(!rst_n),
		.rgb({red, green, blue}),
		.audio_sample_word_in({audioL, audioR}),
		.tmds({tmds_ch2_serial, tmds_ch1_serial, tmds_ch0_serial}),
		.tmds_clock(tmds_chc_serial),
		.cx(x),
		.cy(y),
		.frame_width(),
		.frame_height(),
		.screen_width(),
		.screen_height()
	);

	// TMDS Buffered Output
	OBUFDS #(.IOSTANDARD("TMDS_33")) tmds_buf_ch0 (.I(tmds_ch0_serial), .O(hdmi_tx_p[0]),  .OB(hdmi_tx_n[0]));
	OBUFDS #(.IOSTANDARD("TMDS_33")) tmds_buf_ch1 (.I(tmds_ch1_serial), .O(hdmi_tx_p[1]),  .OB(hdmi_tx_n[1]));
	OBUFDS #(.IOSTANDARD("TMDS_33")) tmds_buf_ch2 (.I(tmds_ch2_serial), .O(hdmi_tx_p[2]),  .OB(hdmi_tx_n[2]));
	OBUFDS #(.IOSTANDARD("TMDS_33")) tmds_buf_chc (.I(tmds_chc_serial), .O(hdmi_tx_clk_p), .OB(hdmi_tx_clk_n));

endmodule
