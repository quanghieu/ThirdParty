#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <gmp.h>
#include <time.h>
#include "key_hom.h"

const char *p_char = "0xFFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF";
char* predef_nonce = "0xFFFFFFF000000000";
mpz_t first_arg;
mpz_t second_arg;
void exponential(mpz_t res, mpz_t base, char* x,int size){    // x is binary form of head, size is length of x
	int i;
	mpz_t f;
	mpz_init(f);
	mpz_init_set_ui(f,1);

	mpz_t p;
	mpz_init(p);
	mpz_init_set_str(p,p_char,0);
//	gmp_printf("P = %Zd\n",p);
	for(i = 0; i<size; i++){
		mpz_mul(f, f, f);
		mpz_mod(f, f, p);
		if(*(x+i) == 49){
			mpz_mul(f, f, base);
			mpz_mod(f, f, p);
		}
	//	gmp_printf("Loop %d : f = %Zx\n",i,f);
	}
	mpz_add_ui(res,f,0);
	
}
void encrypt(mpz_t res,char* key_buf,mpz_t h_x, int blocknum){
	int i = 0;

	if(blocknum == 1019){
		for(i = 0; i < 2048; i++)
			printf(" %d",key_buf[i]);
	}
	exponential(res, h_x, key_buf, 2048);	
}

void H_x(mpz_t h_x, int blocknum, char* nonce){
	mpz_t gmp_nonce;
	char* binary_head;
	mpz_t base_two;

	mpz_init(base_two);
	mpz_init(h_x);
	mpz_init(gmp_nonce);
	mpz_set_ui(base_two,2);
	mpz_set_str(gmp_nonce,nonce,0);
	mpz_add_ui(gmp_nonce,gmp_nonce,blocknum);
	binary_head = mpz_get_str(NULL, 2, gmp_nonce);
	
//	printf("Binary head: %s\n",binary_head);
	exponential(h_x, base_two,binary_head,64);
	if(blocknum == 1019){
		gmp_printf("H(x) = %Zx\n",h_x);
	}
}

void cipher_mult(mpz_t des, mpz_t src1, mpz_t src2){
	mpz_t p;
	mpz_init(p);
	mpz_init_set_str(p,p_char,0);

	mpz_mul(des,src1,src2);
	mpz_mod(des,des,p);
}

void cipher_decrypt(mpz_t res, int f_seq, char* keyfile,mpz_t ciphertext, char* temp_reverse){
	mpz_t reverse_key;
	FILE *fp;
	char c;
	char key_buf[2051];
	int i = 2;
//	char* reverse_buf;
	mpz_t h;
//	char temp_reverse[2049];
	int k = 0;

	mpz_init(h);

	H_x(h,f_seq,predef_nonce);
	exponential(res,h,temp_reverse,2048);
	cipher_mult(res,res,ciphertext);
	
//	gmp_printf("Reverse key = %Zx\n",reverse_key);
//	gmp_printf("Decrypt result = %Zd\n",res);
}

void file_read(char* file){
	FILE *fp;
	char buffer[2048];
	int i;
	if((fp = fopen(file, "r")) == NULL)
		printf("File not found\n");
	else {
		fseek(fp, 10, SEEK_SET);
		fread(buffer, 2048, 1,fp);
	}

/*	for(i = 0; i < 2048; i++){
		printf("%x ",buffer[i]);
	} */
}
void convert_buff(char* input_buf, char* output_buf){
	unsigned int val;
	int i;
	char table[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	for(i = 0; i < 256; i++){
		val = (unsigned int)input_buf[i];
		output_buf[2*i] = table[ (input_buf[i] & 0xf0 ) >> 4 ];
		output_buf[2*i+1] = table[input_buf[i] & 0x0f];	
	}
}

void convert_binary(char* input_buf, char* output_buf){
	int i = 0;
	char temp_buff[2049];
	memset(temp_buff,'0',2048);
	while(*(input_buf + i) != '\0'){
		i++;
	};
	//printf("Length of input char is %d\n",i);
	strcpy(temp_buff + 2048 -i, input_buf);
	for(i = 255; i >= 0; i--){
		output_buf[i] = (char) (128 * (temp_buff[8*i] - 48)
					+ 64 *(temp_buff[8*i + 1] - 48)
					+ 32 *(temp_buff[8*i + 2] - 48)
					+ 16 *(temp_buff[8*i + 3] - 48)
					+ 8 * (temp_buff[8*i + 4] - 48)
					+ 4 * (temp_buff[8*i + 5] - 48)
					+ 2 * (temp_buff[8*i + 6] - 48)
					+ (temp_buff[8*i + 7] - 48));
	}	
}

int full_decrypt(const char* cryptFile, const char* keyfile, const char* decryptFile){
	clock_t start = clock();
	clock_t end;
	FILE *fp;
	FILE *fp_out,*fp_key;
	mpz_t h;
	int last_round = 0;
	int f_seq = 0;
	char* key_buf;
	char c;
	mpz_t reverse_key;
	char* reverse_buf;
	char temp_reverse[2049];
	int i = 2, k = 0;
	mpz_t p;

	mpz_init(h);

	memset(temp_reverse,0,2049);
	if((fp = fopen(cryptFile,"r")) == NULL){
		printf("No file\n");
		return 0;
	}
	
	// Init p
	mpz_init(p);
	mpz_set_str(p,p_char,0);
	// Read key
	key_buf = malloc(2050);
	key_buf[0] = '0';
	key_buf[1] = 'b';
	key_buf[2049] = '\0';
	fp_key = fopen(keyfile,"r");
	if(fp_key != NULL){
		while(1) {
			c = fgetc(fp_key);
			if(c!=EOF){
				key_buf[i] = c;
				i++;
			}
			else
				break;
		}
	}
	// Calculate decrypt key
	mpz_init(reverse_key);

	printf("\nKey buf = %s",key_buf);
	mpz_set_str(reverse_key,key_buf,0);
//	gmp_printf("\n Reverse_buf 1 = %Zx\n",reverse_key);

	mpz_sub(reverse_key,p,reverse_key);
	
	mpz_sub_ui(reverse_key,reverse_key,1);
//	gmp_printf("\n Reverse_buf 2 = %Zx\n",reverse_key);

	reverse_buf = mpz_get_str(NULL,2,reverse_key);
		memset(temp_reverse,'0',2048);
	while(*(reverse_buf + k) != '\0'){
		k++;
	};
	//printf("Length of input char is %d\n",i);
	strcpy(temp_reverse + 2048 -k, reverse_buf);
	printf("Decrypt key = %s\n", temp_reverse);

	fp_out = fopen(decryptFile,"wb");
	while(!last_round)
	{
		int ack;
		char text_buf[515];
		char* block_cipher_bin; 
		char block_cipher[257];
		mpz_t ciphertext;
		char buffer[256];
		
		mpz_t f_text;
		mpz_t res;
		mpz_init(ciphertext);
		mpz_init(res);
		mpz_init(f_text);

		memset(text_buf,0,515);
		memset(block_cipher,0,257);
		memset(buffer,0, 256);

		text_buf[0] = '0';
		text_buf[1] = 'x';
		block_cipher[256] = '\0';
		fseek(fp, f_seq * 256, SEEK_SET);
		ack = fread(buffer, 1, 256,fp);
		//printf("ack = %d\n",ack);
		if(ack != 256){
			break;
		}
		convert_buff(buffer,text_buf+2);
		text_buf[514] = '\0';
		mpz_set_str(ciphertext, text_buf, 0);
//		gmp_printf("Cipher text: %Zx",ciphertext);
		cipher_decrypt(res, f_seq, "keyfile1.txt", ciphertext, temp_reverse);
		if(f_seq == 1019){
			gmp_printf("Decrypt value = %Zd\n",res);
		}
		//printf("Convert result: %s\n",text_buf);
//		mpz_set_str(f_text, text_buf, 0);
//		cipher_mult(res,res, f_text);
		block_cipher_bin = mpz_get_str(NULL,2,res);
		//printf("Block cipher binary: %s\n",block_cipher_bin);
		convert_binary(block_cipher_bin, block_cipher);
/*		for(j = 0; j < 256; j++){
			printf("%d ",block_cipher[j]);
		}*/
		fseek(fp_out, f_seq * 256, SEEK_SET);	
		fwrite(block_cipher, 1,256, fp_out);
//		fwrite(buffer,1, 256, crypt_fp);
		f_seq++;
	}
	end = clock();
	printf("Elapse time = %f\n",(end - start) / (double)CLOCKS_PER_SEC); 
 
	return 1;
}


// Experiment first with blocknum = 1 and nonce = 64 bit 1
int totalEncrypt(char* inputFile, char* keyfile, char* outputFile){
	clock_t start = clock();
	clock_t end;
	int x;
	int k = 0;
	int length;
	FILE *fp,*crypt_fp;
	int file_size,f_seq=0;
	char* outFile; 
	int last_round = 0;
	int j;
	int option;

	FILE *fp_key;
	char c;
	char *key_buf;
	int i = 0;

	mpz_t plaintext;
	mpz_t p;
	mpz_t h;
	mpz_t res1;
	mpz_t res2;
	mpz_t res3;
	mpz_t res4;

	mpz_init(first_arg);
	mpz_init(second_arg);
	mpz_init(p);
	mpz_init(h);
	mpz_init(res1);
	mpz_init(res2);
	mpz_init(res3);
	mpz_init(res4);
	mpz_init_set_ui(plaintext,123456789);
	mpz_init_set_str(p, p_char,0);
//	mpz_t q = (p - 1)>>1;
//	gmp_printf("%Zd\n",p);

	/* Initial Encrypt with key 1*/
/*	H_x(h,1,predef_nonce);
	encrypt(res1,"keyfile1.txt",h);
	cipher_mult(res1,res1,plaintext);
	gmp_printf("Res1 = %Zx\n",res1);


	encrypt(res2,"keyfile2.txt",h);
	gmp_printf("Res2 = %Zx\n",res2);

	cipher_mult(res3,res1,res2);
	gmp_printf("Res3 = %Zx\n",res3);

	cipher_decrypt(res4,"keyfile1_2.txt",res3);
*/
	/* Printf filestream */

	if((fp = fopen(inputFile, "r")) == NULL){
		printf("File not found\n");
		return 1;
	}
	// Read key file
	fp_key = fopen(keyfile,"r");
	
	key_buf = malloc(2049);
	key_buf[2048] = '\0';
	if(fp_key != NULL){
		while(1) {
			c = fgetc(fp_key);
			if(c!=EOF){
				key_buf[i] = c;
				i++;
			}
			else
				break;
		}
	}

	gmp_printf("P = %Zd\n",p);	
	/* File size and allocate a buffer*/
	fseek(fp, 0, SEEK_END);
	rewind(fp);
	file_size = ftell(fp);
	outFile = malloc(file_size);

	crypt_fp = fopen(outputFile,"wb");
	
	while(!last_round)
	{
		int ack;
		char text_buf[515];
		char* block_cipher_bin; 
		char block_cipher[257];
		char buffer[257];

		memset(text_buf,0,515);
		memset(block_cipher,0,257);
		memset(buffer,0, 257);
		
		mpz_t f_text;
		mpz_t res;
		mpz_init(res);
		mpz_init(f_text);
		text_buf[0] = '0';
		text_buf[1] = 'x';
		buffer[256] = '\0';
		block_cipher[256] = '\0';
		fseek(fp, f_seq * 256, SEEK_SET);
		ack = fread(buffer, 1, 256,fp);
//		int l = 0;
//		while(buffer[l] != '\0'){
//			printf(" %x",buffer[l]);
//			l++;
//		}
		//printf("ack = %d\n",ack);
		if(ack != 256){
			last_round = 1;
		}
		H_x(h,f_seq,predef_nonce);
		encrypt(res, key_buf, h, f_seq);
		if(f_seq == 1019){	
			gmp_printf("Cipher text = %Zx\n",res);
			gmp_printf("Res = %Zx\n",res);
		}

		convert_buff(buffer,text_buf+2);
		text_buf[514] = '\0';
		//printf("Convert result: %s\n",text_buf);
		mpz_set_str(f_text, text_buf, 0);
		if(f_seq == 1019){
			gmp_printf("f_text = %Zx\n",f_text);
		}
		cipher_mult(res,res, f_text);
		if(f_seq == 1019){
			gmp_printf("Res 2 = %Zx\n",res);
		}
		block_cipher_bin = mpz_get_str(NULL,2,res);
		//printf("Block cipher binary: %s\n",block_cipher_bin);
		convert_binary(block_cipher_bin, block_cipher);
/*		for(j = 0; j < 256; j++){
			printf("%d ",block_cipher[j]);
		}*/
		fseek(crypt_fp, f_seq * 256, SEEK_SET);	
		fwrite(block_cipher, 1,256, crypt_fp);
//		fwrite(buffer,1, 256, crypt_fp);
		f_seq++;
	} //while(0);
	

	end = clock();
	printf("Elapse time = %f\n",(end - start) / (double)CLOCKS_PER_SEC); 

	return 1;
}

int main(int argc, char *argv[]){
	int option;
	char* inputFile;
	char* keyFile;
	char* outputFile;
//	scanf("%d", &option);
	if(argc < 3){
		printf("Too little arguments\n");
	}

	inputFile = argv[1];
	keyFile = argv[2];
	outputFile = argv[3];
	option = 1;
	printf("%s %s %s",inputFile, keyFile, outputFile);
	if(!option){
		totalEncrypt(inputFile, keyFile, outputFile);
//		totalEncrypt("sample.jpg","keyfile1.txt","photo.jpg");
//		totalEncrypt("zImage", "keyfile2.txt","crypt2.jpg");
	}
	else
		full_decrypt(inputFile,keyFile,outputFile);
}
