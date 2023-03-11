`timescale 1ns / 1ps

module Display (
	input  wire clk,
	input  wire clk_pix1,
	input  wire clk_pix5,
	input  wire clk30,
	input  wire [7:0] sw,
	input  wire buttonL,
	input  wire buttonR,
	input  wire rst_n,
	inout  wire hdmi_tx_cec,     // CE control bidirectional
	input  wire hdmi_tx_hpd,     // hot-plug detect
	inout  wire hdmi_tx_rscl,    // DDC bidirectional
	inout  wire hdmi_tx_rsda,    // DDC bidirectional
	input  wire [23:0] audioL,
	input  wire [23:0] audioR,
	output wire hdmi_tx_clk_n,   // HDMI clock differential negative
	output wire hdmi_tx_clk_p,   // HDMI clock differential positive
	output wire [2:0] hdmi_tx_n, // Three HDMI channels differential negative
	output wire [2:0] hdmi_tx_p, // Three HDMI channels differential positive
	input  wire nesA,
	input  wire nesB,
	input  wire nesSelect,
	input  wire nesStart,
	input  wire nesUp,
	input  wire nesDown,
	input  wire nesLeft,
	input  wire nesRight,
	input  wire useNES,
	output wire useNESOut,
	output wire useNESOutValid,
	input  wire [4:0] multiplier,
	input  wire rx_ready,
	input  wire [7:0] rx_byte,
	input  wire [15:0] gb_channels,
	input  wire [15:0] nes_channels,
	output wire [4:0] jb
);

	wire pix_clk;    // pixel clock
	wire pix_clk_5x; // 5x clock for 10:1 DDR SerDes

	assign pix_clk = clk_pix1;
	assign pix_clk_5x = clk_pix5;

	wire [7:0] red;
	wire [7:0] green;
	wire [7:0] blue;

	wire [10:0] cx;
	wire [9:0] cy;
	wire pulseL;
	wire pulseR;

	wire nesAPulse;
	wire nesBPulse;
	wire nesSelectPulse;
	wire nesStartPulse;
	wire nesUpPulse;
	wire nesDownPulse;
	wire nesLeftPulse;
	wire nesRightPulse;

	Debouncer2 dbuttons (
		.clock(pix_clk),
		.reset(~rst_n),
		.io_in_0(buttonL),
		.io_in_1(buttonR),
		.io_out_0(pulseL),
		.io_out_1(pulseR)
	);

	Debouncer8 debounce_nes (
		.clock(pix_clk),
		.reset(~rst_n),
		.io_in_0(nesA),
		.io_in_1(nesB),
		.io_in_2(nesSelect),
		.io_in_3(nesStart),
		.io_in_4(nesUp),
		.io_in_5(nesDown),
		.io_in_6(nesLeft),
		.io_in_7(nesRight),
		.io_out_0(nesAPulse),
		.io_out_1(nesBPulse),
		.io_out_2(nesSelectPulse),
		.io_out_3(nesStartPulse),
		.io_out_4(nesUpPulse),
		.io_out_5(nesDownPulse),
		.io_out_6(nesLeftPulse),
		.io_out_7(nesRightPulse)
	);

	ImageOutput image_output (
		.clock(pix_clk),
		.reset(~rst_n),
		.io_audioClock(clk_audio_buf),
		.io_x(cx),
		.io_y(cy),
		.io_sw(sw),
		.io_pulseL(pulseL),
		.io_pulseR(pulseR),
		.io_red(red),
		.io_green(green),
		.io_blue(blue),
		.io_nesButtons_a(nesAPulse),
		.io_nesButtons_b(nesBPulse),
		.io_nesButtons_select(nesSelectPulse),
		.io_nesButtons_start(nesStartPulse),
		.io_nesButtons_up(nesUpPulse),
		.io_nesButtons_down(nesDownPulse),
		.io_nesButtons_left(nesLeftPulse),
		.io_nesButtons_right(nesRightPulse),
		.io_useNES(useNES),
		.io_useNESOut_valid(useNESOutValid),
		.io_useNESOut_bits(useNESOut),
		.io_rxByte_valid(rx_ready),
		.io_rxByte_bits(rx_byte),
		.io_multiplier(multiplier),
		.io_gbChannels_0(gb_channels[3:0]),
		.io_gbChannels_1(gb_channels[7:4]),
		.io_gbChannels_2(gb_channels[11:8]),
		.io_gbChannels_3(gb_channels[15:12]),
		.io_nesChannels_0(nes_channels[3:0]),
		.io_nesChannels_1(nes_channels[7:4]),
		.io_nesChannels_2(nes_channels[11:8]),
		.io_nesChannels_3(nes_channels[15:12]),
		.io_jb0(jb[0]),
		.io_jb1(jb[1]),
		.io_jb2(jb[2]),
		.io_jb3(jb[3]),
		.io_jb4(jb[4])
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
		.clk_pixel_x5(pix_clk_5x),
		.clk_pixel(pix_clk),
		.clk_audio(clk_audio_buf),
		.reset(!rst_n),
		.rgb({red, green, blue}),
		.audio_sample_word_in({audioL, audioR}),
		.tmds({tmds_ch2_serial, tmds_ch1_serial, tmds_ch0_serial}),
		.tmds_clock(tmds_chc_serial),
		.cx(cx),
		.cy(cy),
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

	assign hdmi_tx_cec   = 1'bz;
	assign hdmi_tx_rsda  = 1'bz;
	assign hdmi_tx_rscl  = 1'b1;

endmodule
