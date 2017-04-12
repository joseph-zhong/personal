int main() {
  int count = -1;
  cudaGetDeviceCount(&count);
  printf(count);
}
