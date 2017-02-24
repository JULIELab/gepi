import os
import sys
import glob
import pickle


def get_file_list(root):
    for fi in glob.glob(os.path.abspath(root) + "/*"):
        yield os.path.basename(fi)


def is_error(root, fi):
    if os.path.getsize(os.path.abspath(os.path.join(root, fi))) > 0:
        return True
    return False


def dump_list():
    pass


if __name__ == "__main__":
    err_list = list()
    dmp_list = False

    if len(sys.argv) > 1:
        root_folder = sys.argv[1]
    if len(sys.argv) > 2:
        dmp_list = True

    for doc in get_file_list(root_folder):
        if is_error(root_folder, doc):
            err_list.append(doc)
            print(doc)

    if dmp_list:
        with open("err_list.pickle", "wb") as pilist:
            pickle.dump(err_list, pilist)
