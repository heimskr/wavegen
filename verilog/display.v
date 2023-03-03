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
	output wire [2:0] hdmi_tx_p  // Three HDMI channels differential positive
);

	wire pix_clk;    // pixel clock
	wire pix_clk_5x; // 5x clock for 10:1 DDR SerDes

	assign pix_clk = clk_pix1;
	assign pix_clk_5x = clk_pix5;

	wire [7:0] red;
	wire [7:0] green;
	wire [7:0] blue;

	wire [14:0] rom_addr;
	wire [3:0] rom_out;

	wire [10:0] cx;
	wire [9:0] cy;

	image_rom rom (
		.clka(pix_clk),
		.addra(rom_addr),
		.douta(rom_out)
	);

	ImageOutput image_output (
		.clock(pix_clk),
		.reset(~rst_n),
		.io_x(cx),
		.io_y(cy),
		.io_sw(sw),
		.io_buttonL(buttonL),
		.io_buttonR(buttonR),
		.io_addr(rom_addr),
		.io_rom(rom_out),
		.io_red(red),
		.io_green(green),
		.io_blue(blue)
	);

	// TMDS Encoding and Serialization
	wire tmds_ch0_serial, tmds_ch1_serial, tmds_ch2_serial, tmds_chc_serial;
	wire clk_audio;

	reg [9:0] counter = 1'd0;
	always @(posedge clk30) begin
		counter <= counter == 10'd625 ? 1'd0 : counter + 1'd1;
	end

	assign clk_audio = clk30 && counter == 10'd625;

	hdmi #(
		.VIDEO_ID_CODE(4),
		.AUDIO_BIT_WIDTH(24),
		.VENDOR_NAME({"Heimskr", 8'b0}),
		.AUDIO_RATE(48000)
	) magic (
		.clk_pixel_x5(pix_clk_5x),
		.clk_pixel(pix_clk),
		.clk_audio(clk_audio),
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
