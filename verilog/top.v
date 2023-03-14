`timescale 1 ps / 1 ps

module top (
	input  clk,
	input  btnc,
	input  btnd,
	input  btnl,
	input  btnr,
	input  btnu,
	input  cpu_resetn,
	output [7:0] led,
	input  [7:0] sw,
	output ac_mclk,
	input  ac_adc_sdata,
	output ac_dac_sdata,
	output ac_bclk,
	output ac_lrclk,
	inout  scl,
	inout  sda,
	output hdmi_tx_cec,     // CE control bidirectional
	input  hdmi_tx_hpd,     // hot-plug detect
	output hdmi_tx_rscl,    // DDC bidirectional
	output hdmi_tx_rsda,    // DDC bidirectional
	output hdmi_tx_clk_n,   // HDMI clock differential negative
	output hdmi_tx_clk_p,   // HDMI clock differential positive
	output [2:0] hdmi_tx_n, // Three HDMI channels differential negative
	output [2:0] hdmi_tx_p, // Three HDMI channels differential positive
	inout  [7:0] ja, // Pmod JA connector
	output [4:0] jb,
	output uart_rx_out,
	input  uart_tx_in,
	// output [14:0] ddr3_addr,
	// output [2:0] ddr3_ba,
	// output ddr3_ras_n,
	// output ddr3_cas_n,
	// output ddr3_reset_n,
	// output ddr3_we_n,
	// output ddr3_ck_p,
	// output ddr3_ck_n,
	// output ddr3_cke,
	// output [1:0] ddr3_dm,
	// output ddr3_odt,
	// inout  [15:0] ddr3_dq,
	// inout  [1:0] ddr3_dqs_p,
	// inout  [1:0] ddr3_dqs_n
	input sd_cd,
	output sd_reset,
	output sd_cclk,
	output sd_cmd,
	inout [3:0] sd_d
);

	wire clk12MHz;
	wire clk30MHz;
	wire clk50MHz;
	wire clk200MHz;
	wire clk25MHz;

	clk_wiz_0 clk_0 (
		.clk(clk),
		.reset(!cpu_resetn),
		.clk12(clk12MHz),
		.clk30(clk30MHz),
		.clk50(clk50MHz),
		.clk200(clk200MHz),
		.clk25(clk25MHz)
	);

	wire clk_pix1;
	wire clk_pix5;

	clk_wiz_hdmi clk_hdmi (
		.clk(clk),
		.reset(!cpu_resetn),
		.clk_pix1(clk_pix1),
		.clk_pix5(clk_pix5)
	);

	// wire clk_gbx4;
	// wire clk_nesx16;

	// clk_wiz_cpu clk_cpu (
	// 	.clk(clk),
	// 	.reset(!cpu_resetn),
	// 	.clk_gbx4(clk_gbx4),
	// 	.clk_nesx16(clk_nesx16)
	// );

	// reg [1:0] counter_gb;
	// reg clk_gb_slow;
	// reg [1:0] clk_gb_state; // (0 or 2) -> armed, 1 -> firing, 3 -> disarmed
	// wire clk_gb_fast;
	// wire clk_gb_buf;

	// always @(posedge clk_gbx4) begin
	// 	if (counter_gb == 2'd3) begin
	// 		counter_gb  <= 2'd0;
	// 		clk_gb_slow <= 1'b1;
	// 	end else begin
	// 		counter_gb  <= counter_gb + 2'd1;
	// 		clk_gb_slow <= 1'b0;
	// 	end
	// end

	// always @(posedge clk) begin
	// 	if (!cpu_resetn) begin
	// 		clk_gb_state <= 2'd0;
	// 	end else if (!clk_gb_state[0]) begin
	// 		if (clk_gb_slow) begin
	// 			clk_gb_state <= 2'd1;
	// 		end
	// 	end else if (clk_gb_state == 2'd1) begin
	// 		clk_gb_state <= 2'd3;
	// 	end else begin
	// 		if (!clk_gb_slow) begin
	// 			clk_gb_state <= 2'd0;
	// 		end
	// 	end
	// end

	// assign clk_gb_fast = clk_gb_state == 2'd1;
	// BUFG gb_bufg (.I(clk_gb_fast), .O(clk_gb_buf));

	// reg [3:0] counter_nes;
	// reg clk_nes_slow;
	// reg [1:0] clk_nes_state; // See above
	// wire clk_nes_fast;
	// wire clk_nes_buf;

	// always @(posedge clk_nesx16) begin
	// 	if (counter_nes == 4'd15) begin
	// 		counter_nes  <= 4'd0;
	// 		clk_nes_slow <= 1'b1;
	// 	end else begin
	// 		counter_nes  <= counter_nes + 4'd1;
	// 		clk_nes_slow <= 1'b0;
	// 	end
	// end

	// always @(posedge clk) begin
	// 	if (!cpu_resetn) begin
	// 		clk_nes_state <= 2'd0;
	// 	end else if (!clk_nes_state[0]) begin
	// 		if (clk_nes_slow) begin
	// 			clk_nes_state <= 2'd1;
	// 		end
	// 	end else if (clk_nes_state == 2'd1) begin
	// 		clk_nes_state <= 2'd3;
	// 	end else begin
	// 		if (!clk_nes_slow) begin
	// 			clk_nes_state <= 2'd0;
	// 		end
	// 	end
	// end

	// assign clk_nes_fast = clk_nes_state == 2'd1;
	// BUFG nes_bufg (.I(clk_nes_fast), .O(clk_nes_buf));




	// From Nexys Video looper demo

	wire del_mem;              // Clear delete flag
	wire delete;               // Delete flag
	wire [3:0] delete_bank;    // Bank to delete
	wire [3:0] mem_bank;       // Bank
	wire write_zero;           // Used when deleting
	wire [21:0] current_block; // Block address
	wire [15:0] active;        // Bank is recorded on
	wire [3:0]  current_bank;
	wire [25:0] mem_a;
	assign mem_a = {current_block, mem_bank}; // Address is block*8 + banknumber
	wire [63:0] mem_dq_i;
	wire [63:0] mem_dq_o;
	reg  [63:0] mem_dq_o_b;
	wire mem_cen;
	wire mem_oen;
	wire mem_wen;
	wire read_data_valid;
	reg read_data_valid_d1a;
	reg read_data_valid_d1b;
	reg read_data_valid_d2a;
	reg read_data_valid_d2b;
	wire read_data_valid_rise;
	always @(posedge clk200MHz) begin
		read_data_valid_d1a <= read_data_valid;
		read_data_valid_d1b <= read_data_valid_d1a;
	end

	always @(posedge clk) begin
		read_data_valid_d2a <= read_data_valid_d1a | read_data_valid_d1b;
		read_data_valid_d2b <= read_data_valid_d2a;
	end

	assign read_data_valid_rise = read_data_valid_d2a & ~read_data_valid_d2b;

	always @(posedge clk200MHz) begin
		if (read_data_valid) begin
			mem_dq_o_b <= mem_dq_o;
		end
	end

	wire sd_read;
	wire [7:0] sd_dout;
	wire sd_byte_available;
	wire sd_write;
	wire [7:0] sd_din;
	wire sd_write_ready;
	wire sd_ready;
	wire [31:0] sd_address;

	sd_controller sd (
		.cs(sd_d[3]),
		.mosi(sd_cmd),
		.miso(sd_d[0]),
		.sclk(sd_cclk),
		.rd(sd_read),
		.dout(sd_dout),
		.byte_available(sd_byte_available),
		.wr(sd_write),
		.din(sd_din),
		.ready_for_next_byte(sd_write_ready),
		.reset(!cpu_resetn),
		.ready(sd_ready),
		.address(sd_address),
		.clk(clk),
		.status()
	);

	assign sd_d[1] = 1'b1;
	assign sd_d[2] = 1'b1;
	assign sd_reset  = 1'b0;

	// DDRcontrol ram (
	// 	.clk_200MHz_i(clk200MHz),
	// 	// .clk_200MHz_i(clk),
	// 	.rst_i       (!cpu_resetn),
	// 	// RAM interface
	// 	.ram_a       (mem_a),
	// 	.ram_dq_i    (mem_dq_i),
	// 	.ram_dq_o    (mem_dq_o),
	// 	.ram_cen     (mem_cen),
	// 	.ram_oen     (!mem_oen),
	// 	.ram_wen     (!mem_wen),
	// 	.data_valid  (read_data_valid),
	// 	// ddr3 interface
	// 	.ddr3_addr   (ddr3_addr),
	// 	.ddr3_ba     (ddr3_ba),
	// 	.ddr3_ras_n  (ddr3_ras_n),
	// 	.ddr3_cas_n  (ddr3_cas_n),
	// 	.ddr3_reset_n(ddr3_reset_n),
	// 	.ddr3_we_n   (ddr3_we_n),
	// 	.ddr3_ck_p   (ddr3_ck_p),
	// 	.ddr3_ck_n   (ddr3_ck_n),
	// 	.ddr3_cke    (ddr3_cke),
	// 	.ddr3_dm     (ddr3_dm),
	// 	.ddr3_odt    (ddr3_odt),
	// 	.ddr3_dq     (ddr3_dq),
	// 	.ddr3_dqs_p  (ddr3_dqs_p),
	// 	.ddr3_dqs_n  (ddr3_dqs_n)
	// );

	// reg pulse48kHz;
	// wire lrclkrise;
	// assign lrclkrise = lrclkD1 & ~lrclkD2;
	// reg [3:0] lrclkcnt = 0;
    // reg lrclkD1 = 0;
    // reg lrclkD2 = 0;

    // always @(posedge clk) begin
    //     lrclkD1 <= ac_lrclk;
    //     lrclkD2 <= lrclkD1;
    // end

	// always @(posedge clk) begin
	// 	if (lrclkcnt == 8) begin
	// 		pulse48kHz <= 1;
	// 		lrclkcnt <= 0;
	// 	end else begin
	// 		pulse48kHz <= 0;
	// 	end
	// 	if (lrclkrise)
	// 		lrclkcnt <= lrclkcnt+1;
	// end

	// mem_ctrl mem_controller (
	// 	.clk_100MHz(clk),
	// 	.rst(rst),
	// 	.pulse(pulse48kHz),

	// 	.playing(play),
	// 	.recording(r),
	// 	.read_data_valid(read_data_valid_rise),

	// 	.delete(delete),
	// 	.delete_bank(delete_bank),
	// 	.max_block(max_block),
	// 	.delete_clear(del_mem),
	// 	.RamCEn(mem_cen),
	// 	.RamOEn(mem_oen),
	// 	.RamWEn(mem_wen),
	// 	.write_zero(write_zero),
	// 	.get_data(data_flag),
	// 	.data_ready(data_ready),
	// 	.mix_data(mix_data),

	// 	.addrblock48khz(block48KHz),
	// 	.mem_block_addr(current_block),
	// 	.mem_bank(mem_bank)
	// );





	assign ac_mclk = clk12MHz;

	wire dbu;
	wire dbr;
	wire dbd;
	wire dbl;
	wire dbc;

	audio_init initialize_audio (
		.clk(clk50MHz),
		.rst(!cpu_resetn),
		.sda(sda),
		.scl(scl)
	);

	Debouncer5 dbuttons (
		.clock(clk),
		.reset(!cpu_resetn),
		.io_in_0(btnu),
		.io_in_1(btnr),
		.io_in_2(btnl),
		.io_in_3(btnd),
		.io_in_4(btnc),
		.io_out_0(dbu),
		.io_out_1(dbr),
		.io_out_2(dbl),
		.io_out_3(dbd),
		.io_out_4(dbc)
	);

	wire [23:0] out_audioL;
	wire [23:0] out_audioR;

	wire [17:0] rom_addr_gb;
	wire [23:0] rom_out_gb;
	reg  [17:0] rom_addr_gb_reg;

	always @(posedge clk) begin
		rom_addr_gb_reg <= rom_addr_gb;
	end

	blk_mem_gen_0 gb_rom (
		.clka(clk),
		.addra(rom_addr_gb_reg),
		.douta(rom_out_gb)
	);

	wire [16:0] rom_addr_nes;
	wire [23:0] rom_out_nes;

	NesROM nes_rom (
		.clka(clk),
		.addra(rom_addr_nes),
		.douta(rom_out_nes)
	);

	// wire [7:0] rx_byte;
	// wire rx_ready;
	// wire rx_error;
	// reg rx_ack;
	// wire [7:0] tx_byte;
	// wire tx_ready;

	// always @(posedge clk) begin
	// 	rx_ack <= cpu_resetn & rx_ready;
	// end

	// UART #(.FREQ(100_000_000)) uart_thing (
	// 	.reset(cpu_resetn),
	// 	.clk(clk),
	// 	.rx_i(uart_tx_in ^ sw[0]),
	// 	.rx_data_o(rx_byte),
	// 	.rx_ready_o(rx_ready),
	// 	.rx_ack_i(rx_ack),
	// 	.rx_error_o(rx_error),
	// 	.tx_o(uart_rx_out),
	// 	.tx_data_i(tx_byte),
	// 	.tx_ready_i(tx_ready),
	// 	.tx_ack_o()
	// );

	wire nes_a;
	wire nes_b;
	wire nes_select;
	wire nes_start;
	wire nes_up;
	wire nes_down;
	wire nes_left;
	wire nes_right;
	wire use_nes;

	wire use_nes_in;
	reg  use_nes_in_stored;
	wire use_nes_in_valid;
	reg  use_nes_in_valid_stored;

	always @(posedge clk_pix1) begin
		if (!cpu_resetn) begin
			use_nes_in_stored <= 1'b0;
			use_nes_in_valid_stored <= 1'b0;
		end else if (use_nes_in_valid) begin
			use_nes_in_stored <= use_nes_in;
			use_nes_in_valid_stored <= 1'b1;
		end else begin
			use_nes_in_valid_stored <= 1'b0;
		end
	end

	wire [4:0] multiplier;

	wire [3:0] gb_channels  [3:0];
	wire [3:0] nes_channels [3:0];

	MainBoth main_module_both (
		.clock(clk),
		.reset(!cpu_resetn),
		.io_pixClock(clk_pix1),
		// .io_clockNES(clk_nes_buf),
		// .io_clockGB(clk_gb_buf),
		.io_clockNES(1'b0),
		.io_clockGB(1'b0),
		.io_pulseU(dbu),
		.io_pulseR(dbr),
		.io_pulseL(dbl),
		.io_pulseD(dbd),
		.io_pulseC(dbc),
		.io_sw(sw),
		.io_outL(out_audioL),
		.io_outR(out_audioR),
		.io_led(led),
		.io_addrGB(rom_addr_gb),
		.io_addrNES(rom_addr_nes),
		.io_romGB(rom_out_gb),
		.io_romNES(rom_out_nes),
		.io_jaIn(ja),
		.io_pulseOut(ja[3]),
		.io_latchOut(ja[2]),
		// .io_rxByte_valid(rx_ready),
		// .io_rxByte_bits(rx_byte),
		.io_rxByte_valid(1'b0),
		.io_rxByte_bits(8'b0),
		// .io_txByte_valid(tx_ready),
		// .io_txByte_bits(tx_byte),
		.io_nesButtons_a(nes_a),
		.io_nesButtons_b(nes_b),
		.io_nesButtons_select(nes_select),
		.io_nesButtons_start(nes_start),
		.io_nesButtons_up(nes_up),
		.io_nesButtons_down(nes_down),
		.io_nesButtons_left(nes_left),
		.io_nesButtons_right(nes_right),
		.io_useNES(use_nes),
		.io_useNESIn_valid(use_nes_in_valid_stored),
		.io_useNESIn_bits(use_nes_in_stored),
		.io_multiplier(multiplier),
		.io_gbChannels_0(gb_channels[0]),
		.io_gbChannels_1(gb_channels[1]),
		.io_gbChannels_2(gb_channels[2]),
		.io_gbChannels_3(gb_channels[3]),
		.io_nesChannels_0(nes_channels[0]),
		.io_nesChannels_1(nes_channels[1]),
		.io_nesChannels_2(nes_channels[2]),
		.io_nesChannels_3(nes_channels[3])
		// .io_ram_readData_valid(read_data_valid_rise),
		// .io_ram_readData_bits(mem_dq_o_b),
		// .io_ram_readData_ready(mem_oen),
		// .io_ram_writeData_valid(mem_wen),
		// .io_ram_writeData_bits(mem_dq_i),
		// .io_ram_block(current_block),
		// .io_ram_bank(mem_bank),
		// .io_ram_cen(mem_cen)
	);

	i2s_ctl audio_inout (
		.CLK_I(clk),    // Sys clk
		.RST_I(!cpu_resetn),    // Sys rst
		.EN_TX_I(1),  // Transmit Enable (push sound data into chip)
		.EN_RX_I(0), // Receive enable (pull sound data out of chip)
		// .FS_I(4'b0101), // Sampling rate selector
		.FS_I(4'b0000), // Sampling rate selector
		.MM_I(0),     // Audio controller Master mode select
		.D_L_I(out_audioL),    // Left channel data input from mix (mixed audio output)
		.D_R_I(out_audioR),   // Right channel data input from mix
		.D_L_O(in_audioL),    // Left channel data (input from mic input)
		.D_R_O(in_audioR),    // Right channel data (input from mic input)
		.BCLK_O(ac_bclk),   // serial CLK
		.LRCLK_O(ac_lrclk),  // channel CLK
		.SDATA_O(ac_dac_sdata),  // Output serial data
		.SDATA_I(ac_adc_sdata)   // Input serial data
	);

	reg [23:0] storedL;
	reg [23:0] storedR;

	always @(posedge clk) begin
		storedL <= out_audioL;
		storedR <= out_audioR;
	end

	Display display (
		.clk(clk),
		.clk_pix1(clk_pix1),
		.clk_pix5(clk_pix5),
		.sw(sw),
		.buttonL(btnl),
		.buttonR(btnr),
		.clk30(clk30MHz),
		.rst_n(cpu_resetn),
		.hdmi_tx_cec(hdmi_tx_cec),
		.hdmi_tx_hpd(hdmi_tx_hpd),
		.hdmi_tx_rscl(hdmi_tx_rscl),
		.hdmi_tx_rsda(hdmi_tx_rsda),
		.hdmi_tx_clk_n(hdmi_tx_clk_n),
		.hdmi_tx_clk_p(hdmi_tx_clk_p),
		.hdmi_tx_n(hdmi_tx_n),
		.hdmi_tx_p(hdmi_tx_p),
		.audioL(storedL),
		.audioR(storedR),
		.nesA(nes_a),
		.nesB(nes_b),
		.nesSelect(nes_select),
		.nesStart(nes_start),
		.nesUp(nes_up),
		.nesDown(nes_down),
		.nesLeft(nes_left),
		.nesRight(nes_right),
		.useNES(use_nes),
		.useNESOutValid(use_nes_in_valid),
		.useNESOut(use_nes_in),
		// .rx_ready(rx_ready),
		// .rx_byte(rx_byte),
		.rx_ready(1'b0),
		.rx_byte(8'b0),
		.multiplier(multiplier),
		.gb_channels({gb_channels[3], gb_channels[2], gb_channels[1], gb_channels[0]}),
		.nes_channels({nes_channels[3], nes_channels[2], nes_channels[1], nes_channels[0]}),
		.jb(jb),
		.sd_read(sd_read),
		.sd_dout(sd_dout),
		.sd_byte_available(sd_byte_available),
		.sd_write(sd_write),
		.sd_din(sd_din),
		.sd_write_ready(sd_write_ready),
		.sd_ready(sd_ready),
		.sd_address(sd_address)
	);

endmodule
