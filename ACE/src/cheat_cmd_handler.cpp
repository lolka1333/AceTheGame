#include "ACE/cheat_cmd_handler.hpp"
#include "ACE/ace_type.hpp"
#include "ACE/cheat_session.hpp"
#include "ACE/common.hpp"
#include "ACE/maps.hpp"
#include "ACE/proc_rw.hpp"
#include "ACE/to_frontend.hpp"
#include <limits.h>
#include <sys/types.h> //For ssize_t

template <typename T> cheat_mode_args<T>::cheat_mode_args() {

  this->operator_type = Scan_Utils::E_operator_type::equal;
  this->addr_to_read = 0;
  this->addr_to_write = 0;
  this->endian_scan_type = E_endian_scan_type::native;
  this->scan_level_type = Scan_Utils::E_scan_level::aligned_only;
  this->list_max_count = SIZE_MAX;
  this->read_arr_read_length = 0;
}

template <typename T>
void list_cmd_handler(const ACE_scanner<T> *scanner, size_t list_max_count) {
  const match_storage<T> &scan_res = scanner->get_current_scan_result();

  size_t display_count = std::min(list_max_count, scan_res.get_matches_count());

  scan_res.iterate_val(

      [&](ADDR addr, T val) {
        T val_display = val;
        // swap endian again if scan type is swapped
        // for a good normal (as in the endian is native )display
        if (scanner->get_endian_scan_type() == E_endian_scan_type::swapped)
          val_display = swap_endian<T>(val);
        //

        frontend::print("0x%llx %s\n", addr,
                        std::to_string(val_display).c_str());
      },

      display_count

  );
}

template <typename T>
void matchcount_cmd_handler(const ACE_scanner<T> *scanner) {
  const match_storage<T> &scan_res = scanner->get_current_scan_result();
  frontend::print("%zu\n", scan_res.get_matches_count());
}

void pid_cmd_handler(int pid) {
  //
  frontend::print("%d\n", pid);
}
template <typename T>
void next_scan_cmd_handler(ACE_scanner<T> *scanner,
                           Scan_Utils::E_operator_type operator_type,
                           const cheat_mode_config *cheat_config) {
  if (!scanner->get_first_scan_done())
    frontend::print("WARN: no initial scan has been setup\n");

  double next_scan_time = -1;
  time_action(

      [&]() {
        //
        scanner->next_scan(operator_type);
      },
      &next_scan_time

  );

  frontend::print("current matches: %zu\n",
                  scanner->get_current_scan_result().get_matches_count());
  frontend::print("Done in: %lf s\n", next_scan_time);
}

template <typename T>
void scan_cmd_handler(ACE_scanner<T> *scanner,

                      Scan_Utils::E_operator_type operator_type,

                      cheat_mode_config *cheat_config,

                      T num_to_find) {

  double scan_time = -1;

  time_action(

      [&]() {
        if (!scanner->get_first_scan_done()) {

          scanner->first_scan(

              operator_type, num_to_find,

              [](const std::vector<struct mem_region> &segments_to_scan) {
                frontend::print("Found %zu regions to be scanned\n",
                                segments_to_scan.size());
              }

          );
        }

        else {
          scanner->next_scan(operator_type, num_to_find);
        }
      },

      &scan_time

  );

  frontend::print("current matches: %zu\n",
                  scanner->get_current_scan_result().get_matches_count());
  frontend::print("Done in: %lf s\n", scan_time);
}
template <typename T>
void write_cmd_handler(ACE_scanner<T> *scanner, T val_to_write) {

  /*
   * parse value to write and write to all addresses
   * in the current scan
   * */

  scanner->write_val_to_current_scan_results(val_to_write);
}

template <typename T>
void readat_cmd_handler(proc_rw<T> *process_rw, ADDR address) {

  // reset errno
  errno = 0;
  // read
  T read_val = process_rw->retrieve_val((byte *)address);

  if (errno != 0)
    frontend::print("error while reading: %s\n", strerror(errno));
  else
    frontend::print("%s\n", std::to_string(read_val).c_str());
}

template <typename T>
void read_arr_cmd_handler(proc_rw<T> *process_rw, ADDR address,
                          size_t read_length) {

  byte *mem_buff = (byte *)malloc(sizeof(byte) * read_length);
  errno = 0;
  // retrieve an array of byte
  size_t successfull_read_length = process_rw->read_mem_new(
      (byte *)address, read_length, mem_buff,
      Scan_Utils::E_read_mem_method::with_process_vm_readv);

  // check for some warnings/error
  if (errno != 0) {
    frontend::print("WARN: an error occured %s (%d)\n", strerror(errno), errno);
  }
  if (successfull_read_length != read_length) {
    frontend::print("WARN: cannot read %zu bytes as requested\n", read_length);
    frontend::print("WARN: only read %zu bytes\n", successfull_read_length);
  }

  // print out the read memory
  for (size_t i = 0; i < successfull_read_length; i++) {
    frontend::print("0x%llx ", address + i);
    frontend::print("%s\n", std::to_string(mem_buff[i]).c_str());
  }

  // free allocated memory
  free(mem_buff);
}

template <typename T>
void writeat_cmd_handler(proc_rw<T> *process_rw, ADDR address, T val_to_write) {

  // reset errno
  errno = 0;
  // write
  ssize_t ret_val = process_rw->write_val((byte *)address, (T)val_to_write);

  if (errno != 0 && ret_val == -1) {
    frontend::print("Error while writting at %p: %s\n", (byte *)address,
                    strerror(errno));
    return;
  }
}
template <typename T>
void update_cmd_handler(ACE_scanner<T> *scanner,
                        const cheat_mode_config *cheat_config) {
  if (!scanner->get_first_scan_done()) {
    frontend::print("WARN: No initial scan is done\n");
    return;
  }
  scanner->update_current_scan_result();
  frontend::print("Done updating value!\n");
}

template <typename T>
void endian_cmd_handler(ACE_scanner<T> *scanner,
                        E_endian_scan_type endian_scan_type) {
  scanner->set_endian_scan_type(endian_scan_type);
}

template <typename T>
void scan_level_cmd_handler(ACE_scanner<T> *scanner,
                            Scan_Utils::E_scan_level scan_level) {
  scanner->set_scan_level(scan_level);

  std::string first_scan_level_val =
      Scan_Utils::E_scan_level_to_str.at(scan_level);

  frontend::print("set scan level to %s\n", first_scan_level_val.c_str());
}

void type_cmd_handler(E_num_type num_type,
                      cheat_cmd_ret *cheater_on_line_ret_ptr) {
  cheater_on_line_ret_ptr->set_next_num_type(num_type);
  frontend::print("set num type to %s\n",
                  E_num_type_to_str_map.at(num_type).c_str());
}

template <typename T>
void freeze_at_cmd_handler(freezer<T> *freezer_manager, ADDR addr) {
  int ret_val = freezer_manager->freeze_addr(addr);
  if (ret_val != 0) {
    frontend::print("Fail to freeze address %lld\n", addr);
    return;
  }
}

template <typename T>
void freeze_at_val_cmd_handler(freezer<T> *freezer_manager, ADDR addr,
                               T num_val) {
  int ret_val = freezer_manager->freeze_addr_with_val(addr, num_val);
  if (ret_val != 0) {
    frontend::print("Fail to freeze address %lld\n", addr);
    return;
  }
}

template <typename T>
void unfreeze_at_cmd_handler(freezer<T> *freezer_manager, ADDR addr) {
  int ret_val = freezer_manager->unfreeze_addr(addr);
  if (ret_val != 0) {
    frontend::print("Fail to stop freezing address %lld\n", addr);
    return;
  }
}

template <typename T>
void freeze_all_cmd_handler(const ACE_scanner<T> *scanner,
                            freezer<T> *freezer_manager) {
  const match_storage<T> &scan_result = scanner->get_current_scan_result();
  // freeze all addresses
  scan_result.iterate_val(

      [&](ADDR addr, T val) {
        //
        freezer_manager->freeze_addr(addr);
      }

  );
  frontend::print("freezed all scan's result\n");
}
template <typename T>
void freeze_list_cmd_handler(const freezer<T> *freezer_manager) {
  // TODO: need a function that get
  // properties of an address so that
  // every displays of an address stay consistent

  const std::map<ADDR, thread_continuous> freeze_maps =
      freezer_manager->get_freeze_maps();

  // print out all addresses
  for (auto it = freeze_maps.begin(); it != freeze_maps.end(); it++) {
    frontend::print("0x%llx\n", it->first);
    frontend::print("==========================\n");
  }
}

template <typename T>
void unfreeze_all_cmd_handler(freezer<T> *freezer_manager) {
  freezer_manager->stop_all();
  frontend::print("all previously freezed value stopped\n");
}

/*
 * explicit template function instantiations
 * https://stackoverflow.com/a/4933205/14073678
 */
#define EXPLICIT_INSTANTIATE_CHEAT_CMD_HANDLER(TYPE)                           \
  template struct cheat_mode_args<TYPE>;                                       \
                                                                               \
  template void list_cmd_handler<TYPE>(const ACE_scanner<TYPE> *scanner,       \
                                       size_t list_max_count);                 \
                                                                               \
  template void matchcount_cmd_handler<TYPE>(                                  \
      const ACE_scanner<TYPE> *scanner);                                       \
                                                                               \
  template void next_scan_cmd_handler<TYPE>(                                   \
      ACE_scanner<TYPE> * scanner, Scan_Utils::E_operator_type operator_type,  \
      const cheat_mode_config *cheat_config);                                  \
                                                                               \
  template void scan_cmd_handler<TYPE>(                                        \
      ACE_scanner<TYPE> * scanner, Scan_Utils::E_operator_type operator_type,  \
      cheat_mode_config * cheat_config, TYPE num_to_find);                     \
                                                                               \
  template void write_cmd_handler<TYPE>(ACE_scanner<TYPE> * scanner,           \
                                        TYPE val_to_write);                    \
                                                                               \
  template void update_cmd_handler<TYPE>(                                      \
      ACE_scanner<TYPE> * scanner, const cheat_mode_config *cheat_config);     \
                                                                               \
  template void readat_cmd_handler<TYPE>(proc_rw<TYPE> * process_rw,           \
                                         ADDR address);                        \
                                                                               \
  template void read_arr_cmd_handler(proc_rw<TYPE> *process_rw, ADDR address,  \
                                     size_t read_length);                      \
                                                                               \
  template void writeat_cmd_handler<TYPE>(proc_rw<TYPE> * process_rw,          \
                                          ADDR address, TYPE val_to_write);    \
                                                                               \
  template void endian_cmd_handler<TYPE>(ACE_scanner<TYPE> * scanner,          \
                                         E_endian_scan_type endian_scan_type); \
  template void scan_level_cmd_handler<TYPE>(                                  \
      ACE_scanner<TYPE> * scanner, Scan_Utils::E_scan_level scan_level);       \
  template void freeze_at_cmd_handler(freezer<TYPE> *freezer_manager,          \
                                      ADDR addr);                              \
  template void unfreeze_at_cmd_handler(freezer<TYPE> *freezer_manager,        \
                                        ADDR addr);                            \
  template void freeze_all_cmd_handler(const ACE_scanner<TYPE> *scanner,       \
                                       freezer<TYPE> *freezer_manager);        \
  template void unfreeze_all_cmd_handler(freezer<TYPE> *freezer_manager);      \
  template void freeze_list_cmd_handler(const freezer<TYPE> *freezer_manager); \
  template void freeze_at_val_cmd_handler(freezer<TYPE> *freezer_manager,      \
                                          ADDR addr, TYPE num_val)

// use macro to instantiate
TEMPLATE_NUMERIC_INSTANTIATE(EXPLICIT_INSTANTIATE_CHEAT_CMD_HANDLER);
// undef so it can only be used here
#undef EXPLICIT_INSTANTIATE_CHEAT_CMD_HANDLER
